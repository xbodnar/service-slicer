package cz.bodnor.serviceslicer.application.module.loadtestexperiment.service

import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTest
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTestRepository
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Service
class SystemUnderTestRunner(
    private val sutRepository: SystemUnderTestRepository,
    private val diskOperations: DiskOperations,
    private val remoteProperties: RemoteExecutionProperties,
    localCommandExecutor: LocalCommandExecutor,
    sshCommandExecutor: SshCommandExecutor,
) {
    val commandExecutor = if (remoteProperties.enabled) sshCommandExecutor else localCommandExecutor

    data class RunInfo(
        val id: UUID,
        val project: String,
        val sutService: String,
        var state: RunState,
    )

    enum class RunState { STARTING, WAITING_HEALTHY, RUNNING }

    private val logger = KotlinLogging.logger {}
    private val currentRun = AtomicReference<RunInfo?>(null)

    @Async
    fun startSUT(systemUnderTestId: UUID) {
        val sut = sutRepository.findById(systemUnderTestId).orElseThrow { IllegalStateException("SUT not found") }

        val project = "ss_run_$systemUnderTestId" // compose project name
        val info = RunInfo(systemUnderTestId, project, sut.name, RunState.STARTING)

        // Check if there's already an active run
        if (!currentRun.compareAndSet(null, info)) {
            val activeRun = currentRun.get()
            throw IllegalStateException(
                "Cannot start SUT $systemUnderTestId: Another SUT is already running (${activeRun?.id}, state=${activeRun?.state})",
            )
        }

        diskOperations.withFile(sut.composeFileId) { composeFilePath ->
            logger.info { "Starting SUT from docker-compose file: ${composeFilePath.toFile().absolutePath}" }

            try {
                // 1) Transfer compose file to execution environment (if remote)
                val remoteComposePath = commandExecutor.transferFile(
                    composeFilePath,
                    "$project/compose.yaml",
                )
                val remoteComposeFile = remoteComposePath.toFile()
                val workDir = remoteComposeFile.parentFile

                // 2) Compose up (detached, unique project)
                val result = commandExecutor.execute(
                    listOf("docker", "compose", "-f", remoteComposeFile.absolutePath, "-p", project, "up", "-d"),
                    workDir,
                )
                if (result.exitCode != 0) {
                    throw IllegalStateException("compose up failed (exit=${result.exitCode})\n${result.output}")
                }

                logger.info { "SUT started, checking health..." }

                // 3) Wait for SUT healthy
                info.state = RunState.WAITING_HEALTHY
                waitHealthy(sut.appPort, sut.healthCheckPath, timeout = Duration.ofSeconds(sut.startupTimeoutSeconds))

                logger.info { "SUT is healthy" }

                // 4) Execute SQL seed file if provided
                if (sut.sqlSeedFileId != null) {
                    logger.info { "SQL seed file specified, executing SQL script..." }
                    executeSqlSeedFile(sut, project, workDir)
                    logger.info { "SQL seed file executed successfully" }
                }

                logger.info { "SUT is ready to run tests" }

                info.state = RunState.RUNNING
            } catch (t: Throwable) {
                logger.error(t) { "Failed to start SUT" }

                currentRun.set(null)
            }
        }
    }

    fun status(): RunInfo? = currentRun.get()

    @PreDestroy
    fun stopSUT() {
        val info = currentRun.get() ?: return

        runCatching {
            commandExecutor.execute(
                listOf("docker", "compose", "-p", info.project, "down", "-v"),
                null,
            )
        }

        currentRun.set(null)
    }

    data class SUTContainer(
        val id: UUID,
        val host: String,
        val port: Int,
    )

    // --- helpers ---

    private fun executeSqlSeedFile(
        sut: SystemUnderTest,
        project: String,
        workDir: java.io.File,
    ) {
        // Extract and validate required fields
        val sqlSeedFileId = requireNotNull(sut.sqlSeedFileId) { "SQL seed file ID must be provided" }
        val dbContainerName = requireNotNull(sut.dbContainerName) { "DB container name must be provided" }
        val dbPort = requireNotNull(sut.dbPort) { "DB port must be provided" }
        val dbName = requireNotNull(sut.dbName) { "DB name must be provided" }
        val dbUsername = requireNotNull(sut.dbUsername) { "DB username must be provided" }

        diskOperations.withFile(sqlSeedFileId) { sqlFilePath ->
            logger.info { "Transferring SQL seed file to execution environment..." }

            // Transfer SQL file to execution environment
            val remoteSqlPath = commandExecutor.transferFile(
                sqlFilePath,
                "$project/seed.sql",
            )

            // Copy SQL file into database container
            logger.info { "Copying SQL file into database container $dbContainerName..." }
            val copyResult = commandExecutor.execute(
                listOf(
                    "docker",
                    "compose",
                    "-p",
                    project,
                    "cp",
                    remoteSqlPath.toFile().name,
                    "$dbContainerName:/tmp/seed.sql",
                ),
                workDir,
            )
            if (copyResult.exitCode != 0) {
                throw IllegalStateException(
                    "Failed to copy SQL file into container (exit=${copyResult.exitCode})\n${copyResult.output}",
                )
            }

            // Debug: Check database connection and available databases
            logger.debug { "Running database diagnostics before SQL execution..." }
            debugDatabaseConnection(project, workDir, dbContainerName, dbUsername, dbName)

            // Execute SQL script inside the container
            logger.info { "Executing SQL script in database '$dbName' as user '$dbUsername'..." }
            val psqlCommand = listOf(
                "docker",
                "compose",
                "-p",
                project,
                "exec",
                "-T",
                dbContainerName,
                "psql",
                "-U",
                dbUsername,
                "-d",
                dbName,
                "-v",
                "ON_ERROR_STOP=1", // Stop on first error
                "-a", // Echo all input from script
                "-f",
                "/tmp/seed.sql",
            )
            logger.debug { "Executing command: ${psqlCommand.joinToString(" ")}" }

            val execResult = commandExecutor.execute(psqlCommand, workDir)

            logger.info { "SQL execution output:\n${execResult.output}" }

            if (execResult.exitCode != 0) {
                throw IllegalStateException(
                    "Failed to execute SQL script against database '$dbName' (exit=${execResult.exitCode})\n" +
                        "Command: ${psqlCommand.joinToString(" ")}\n" +
                        "Output:\n${execResult.output}",
                )
            }
        }
    }

    private fun debugDatabaseConnection(
        project: String,
        workDir: java.io.File,
        dbContainerName: String,
        dbUsername: String,
        dbName: String,
    ) {
        // 1. List all databases
        logger.debug { "Listing all databases in container '$dbContainerName'..." }
        val listDbResult = commandExecutor.execute(
            listOf(
                "docker",
                "compose",
                "-p",
                project,
                "exec",
                "-T",
                dbContainerName,
                "psql",
                "-U",
                dbUsername,
                "-l",
            ),
            workDir,
        )
        logger.debug { "Available databases:\n${listDbResult.output}" }

        // 2. Check current database connection
        logger.debug { "Checking connection to database '$dbName'..." }
        val checkDbResult = commandExecutor.execute(
            listOf(
                "docker",
                "compose",
                "-p",
                project,
                "exec",
                "-T",
                dbContainerName,
                "psql",
                "-U",
                dbUsername,
                "-d",
                dbName,
                "-c",
                "SELECT current_database();",
            ),
            workDir,
        )
        logger.debug { "Current database connection:\n${checkDbResult.output}" }

        // 3. Show search_path
        logger.debug { "Checking search_path in database '$dbName'..." }
        val searchPathResult = commandExecutor.execute(
            listOf(
                "docker",
                "compose",
                "-p",
                project,
                "exec",
                "-T",
                dbContainerName,
                "psql",
                "-U",
                dbUsername,
                "-d",
                dbName,
                "-c",
                "SHOW search_path;",
            ),
            workDir,
        )
        logger.debug { "Search path:\n${searchPathResult.output}" }

        // 4. List all schemas
        logger.debug { "Listing all schemas in database '$dbName'..." }
        val listSchemasResult = commandExecutor.execute(
            listOf(
                "docker",
                "compose",
                "-p",
                project,
                "exec",
                "-T",
                dbContainerName,
                "psql",
                "-U",
                dbUsername,
                "-d",
                dbName,
                "-c",
                "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name;",
            ),
            workDir,
        )
        logger.debug { "Available schemas:\n${listSchemasResult.output}" }

        // 5. List tables across ALL schemas
        logger.debug { "Listing ALL tables in database '$dbName' (all schemas)..." }
        val listAllTablesResult = commandExecutor.execute(
            listOf(
                "docker",
                "compose",
                "-p",
                project,
                "exec",
                "-T",
                dbContainerName,
                "psql",
                "-U",
                dbUsername,
                "-d",
                dbName,
                "-c",
                "SELECT schemaname, tablename FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema') ORDER BY schemaname, tablename;",
            ),
            workDir,
        )
        logger.debug { "All tables in database '$dbName':\n${listAllTablesResult.output}" }
    }

    private fun waitHealthy(
        port: Int,
        healthCheckPath: String,
        timeout: Duration,
    ) {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        val targetHost = commandExecutor.getTargetHost()
        val url = URL("http://$targetHost:$port$healthCheckPath")

        while (System.currentTimeMillis() < deadline) {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    logger.info { "SUT is healthy at $url" }
                    return
                }
                logger.debug { "Health check returned $responseCode, retrying..." }
            } catch (e: Exception) {
                logger.debug { "Health check failed: ${e.message}, retrying..." }
            }
            Thread.sleep(1500)
        }
        throw IllegalStateException("Timed out waiting for SUT health at $url")
    }
}
