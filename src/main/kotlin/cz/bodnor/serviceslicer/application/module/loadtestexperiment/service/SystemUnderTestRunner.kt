package cz.bodnor.serviceslicer.application.module.loadtestexperiment.service

import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTestRepository
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
    private val commandExecutor: CommandExecutor,
) {

    data class RunInfo(
        val id: UUID,
        val project: String,
        val sutService: String,
        var state: RunState,
        var exitCode: Int? = null,
        var logsTail: String? = null,
        var summaryJson: String? = null,
    )

    enum class RunState { QUEUED, STARTING, WAITING_HEALTHY, RUNNING, FAILED }

    private val logger = KotlinLogging.logger {}
    private val currentRun = AtomicReference<RunInfo?>(null)

    @Async
    fun startSUT(systemUnderTestId: UUID) {
        val sut = sutRepository.findById(systemUnderTestId).orElseThrow { IllegalStateException("SUT not found") }

        val project = "ss_run_$systemUnderTestId" // compose project name
        val info = RunInfo(systemUnderTestId, project, sut.name, RunState.QUEUED)

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
                    "/tmp/serviceslicer/$project/compose.yaml",
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

                logger.info { "SUT started" }

                // 3) Wait for SUT healthy
                info.state = RunState.WAITING_HEALTHY
                waitHealthy(sut.appPort, sut.healthCheckPath, timeout = Duration.ofSeconds(sut.startupTimeoutSeconds))

                info.state = RunState.RUNNING
            } catch (t: Throwable) {
                logger.error(t) { "Failed to start SUT" }

                // If the run failed or finished, allow cleanup only if it's still the current run
                if (info.state == RunState.FAILED) {
                    currentRun.compareAndSet(info, null)
                }
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
