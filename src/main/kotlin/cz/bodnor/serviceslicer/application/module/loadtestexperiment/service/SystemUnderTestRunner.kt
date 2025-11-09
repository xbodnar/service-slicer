package cz.bodnor.serviceslicer.application.module.loadtestexperiment.service

import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTestRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class SystemUnderTestRunner(
    private val sutRepository: SystemUnderTestRepository,
    private val diskOperations: DiskOperations,
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
    private val runs = ConcurrentHashMap<UUID, RunInfo>()

    @Async
    fun startSUT(
        systemUnderTestId: UUID,
        healthCheckPath: String = "/actuator/health",
        startupTimeout: Duration = Duration.ofMinutes(2),
    ) {
        val sut = sutRepository.findById(systemUnderTestId).orElseThrow { IllegalStateException("SUT not found") }

        val systemPort = 9090

        diskOperations.withFile(sut.composeFileId) { composeFilePath ->
            val project = "ss_run_$systemUnderTestId" // compose project name
            val info = RunInfo(systemUnderTestId, project, sut.name, RunState.QUEUED)
            runs[systemUnderTestId] = info

            val composeFile = composeFilePath.toFile()
            val workDir = composeFile.parentFile

            logger.info { "Starting SUT from docker-compose file: ${composeFile.absolutePath}" }

            try {
                // 1) Compose up (detached, unique project)
                sh(
                    listOf("docker", "compose", "-f", composeFile.absolutePath, "-p", project, "up", "-d"),
                    workDir,
                ).throwIfFail("compose up failed")

                logger.info { "SUT started" }

                // 2) Wait for SUT healthy
                info.state = RunState.WAITING_HEALTHY
                waitHealthy(systemPort, healthCheckPath, timeout = Duration.ofMinutes(3))

                info.state = RunState.RUNNING
            } catch (t: Throwable) {
                logger.error(t) { "Failed to start SUT" }
                info.state = RunState.FAILED
                info.logsTail = t.message
            }
        }
    }

    fun status(systemUnderTestId: UUID) = runs[systemUnderTestId]

    fun stopSUT(
        systemUnderTestId: UUID,
        composeFile: Path,
    ) {
        // 4) Compose down (remove volumes too)
        val info = runs[systemUnderTestId] ?: return

        runCatching {
            sh(
                listOf("docker", "compose", "-f", composeFile.toFile().absolutePath, "-p", info.project, "down", "-v"),
                composeFile.toFile().parentFile,
            )
        }

        runs.remove(systemUnderTestId)
    }

    data class SUTContainer(
        val id: UUID,
        val host: String,
        val port: Int,
    )

    // --- helpers ---

    private data class ShResult(val code: Int, val out: String)

    private fun sh(
        cmd: List<String>,
        dir: File?,
    ): ShResult {
        val pb = ProcessBuilder(cmd)
        if (dir != null) pb.directory(dir)
        pb.redirectErrorStream(true)
        val p = pb.start()
        val out = p.inputStream.bufferedReader().readText()
        val code = p.waitFor()
        return ShResult(code, out)
    }

    private fun ShResult.throwIfFail(msg: String) {
        if (code != 0) throw IllegalStateException("$msg (exit=$code)\n$out")
    }

    private fun waitHealthy(
        port: Int,
        healthCheckPath: String,
        timeout: Duration,
    ) {
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        val url = URL("http://localhost:$port$healthCheckPath")

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
