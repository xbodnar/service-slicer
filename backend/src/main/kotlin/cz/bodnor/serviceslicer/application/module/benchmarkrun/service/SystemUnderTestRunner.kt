package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.benchmark.DockerConfig
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Service
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Service
class SystemUnderTestRunner(
    private val benchmarkReadService: BenchmarkReadService,
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

    fun startSUT(
        benchmarkId: UUID,
        systemUnderTestId: UUID,
    ) {
        val benchmark = benchmarkReadService.getById(benchmarkId)
        val sut = benchmark.getSystemUnderTest(systemUnderTestId)

        val project = "ss_run_$systemUnderTestId" // compose project name
        val info = RunInfo(systemUnderTestId, project, sut.name, RunState.STARTING)

        // Check if there's already an active run
        if (!currentRun.compareAndSet(null, info)) {
            val activeRun = currentRun.get()
            throw IllegalStateException(
                "Cannot start SUT $systemUnderTestId: Another SUT is already running (${activeRun?.id}, state=${activeRun?.state})",
            )
        }

        diskOperations.withFile(sut.dockerConfig.composeFileId) { composeFilePath ->
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

                // 3) Wait for SUT healthy
                info.state = RunState.WAITING_HEALTHY
                waitHealthy(sut.dockerConfig)

                // 4) Execute SQL seed file if provided
                sut.databaseSeedConfig?.let {
                    it.waitForDatabaseSchema(project, workDir)
                    it.executeSqlSeedFile(project, workDir)
                    logger.debug { "SQL seed file executed successfully" }
                }

                logger.info { "SUT is ready to run tests" }

                info.state = RunState.RUNNING
            } catch (t: Throwable) {
                logger.error(t) { "Failed to start SUT" }
                stopSUT()
                throw t
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
            logger.info { "SUT stopped" }
        }

        currentRun.set(null)
    }

    data class SUTContainer(
        val id: UUID,
        val host: String,
        val port: Int,
    )

    // --- helpers ---
    private fun DatabaseSeedConfig.executeSqlSeedFile(
        project: String,
        workDir: File,
    ) {
        diskOperations.withFile(sqlSeedFileId) { sqlFilePath ->
            logger.debug { "Transferring SQL seed file to execution environment..." }

            // Transfer SQL file to execution environment
            val remoteSqlPath = commandExecutor.transferFile(
                sqlFilePath,
                "$project/seed.sql",
            )

            // Copy SQL file into database container
            logger.debug { "Copying SQL file into database container $dbContainerName..." }
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
            if (logger.isDebugEnabled()) {
                debugDatabaseConnection(project, workDir)
            }

            // Execute SQL script inside the container
            logger.debug { "Executing SQL script in database '$dbName' as user '$dbUsername'..." }
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

            logger.debug { "SQL execution output:\n${execResult.output}" }

            if (execResult.exitCode != 0) {
                throw IllegalStateException(
                    "Failed to execute SQL script against database '$dbName' (exit=${execResult.exitCode})\n" +
                        "Command: ${psqlCommand.joinToString(" ")}\n" +
                        "Output:\n${execResult.output}",
                )
            }
        }
    }

    private fun DatabaseSeedConfig.debugDatabaseConnection(
        project: String,
        workDir: File,
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

    private fun DatabaseSeedConfig.waitForDatabaseSchema(
        project: String,
        workDir: File,
    ) {
        val deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis()
        var attemptCount = 0

        while (System.currentTimeMillis() < deadline) {
            attemptCount++
            logger.debug { "Checking if database tables exist (attempt $attemptCount)..." }

            val checkTablesResult = commandExecutor.execute(
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
                    "SELECT COUNT(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog', 'information_schema');",
                ),
                workDir,
            )

            if (checkTablesResult.exitCode == 0) {
                // Parse the count from output (format is: " count \n-------\n   X\n")
                val tableCount = checkTablesResult.output
                    .lines()
                    .dropWhile { !it.contains("---") }
                    .drop(1)
                    .firstOrNull()
                    ?.trim()
                    ?.toIntOrNull() ?: 0

                logger.debug { "Found $tableCount user tables in database" }

                if (tableCount > 0) {
                    logger.info { "Database schema is ready with $tableCount tables" }
                    return
                }
            }

            logger.debug { "No tables found yet, waiting..." }
            Thread.sleep(2000)
        }

        throw IllegalStateException(
            "Timed out waiting for database schema to be created. " +
                "Make sure your application creates tables before the SQL seed file runs.",
        )
    }

    private fun waitHealthy(dockerConfig: DockerConfig) {
        val deadline = System.currentTimeMillis() + dockerConfig.startupTimeoutSeconds * 1000
        val targetHost = commandExecutor.getTargetHost()
        val url = URL("http://$targetHost:${dockerConfig.appPort}${dockerConfig.healthCheckPath}")

        while (System.currentTimeMillis() < deadline) {
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 2000
                connection.readTimeout = 2000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    logger.debug { "SUT is healthy at $url" }
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
