package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

@Service
class K6Runner(
    private val k6CommandExecutor: K6CommandExecutor,
    private val sshTunnelManager: SshTunnelManager,
) {

    private val logger = KotlinLogging.logger {}

    data class K6Result(
        val startTime: Instant,
        val endTime: Instant,
        val output: String,
        val summaryJson: JsonNode? = null,
    )

    fun runValidation(
        operationalSettingId: UUID,
        appPort: Int,
    ): K6Result {
        logger.info { "Executing k6 validation run..." }

        return sshTunnelManager.withOptionalTunnel(appPort) { port ->
            val startTime = Instant.now()
            val result = k6CommandExecutor.executeValidation(operationalSettingId, port)
            val endTime = Instant.now()

            if (result.exitCode != 0) {
                error("k6 validation failed with exit code ${result.exitCode}, output:\n${result.output}")
            }

            K6Result(
                startTime = startTime,
                endTime = endTime,
                output = result.output,
                summaryJson = null,
            )
        }
    }

    fun runTest(
        benchmarkRunId: UUID,
        testCaseId: UUID,
        appPort: Int,
        load: Int,
        testDuration: String,
    ): K6Result {
        logger.info { "Executing k6 test run..." }

        return sshTunnelManager.withOptionalTunnel(appPort) { port ->

            // Warmup run
            k6CommandExecutor.executeK6WarmUp(benchmarkRunId, testCaseId, port, load)

            // Main run
            val startTime = Instant.now()
            val result = k6CommandExecutor.executeK6Test(
                benchmarkRunId,
                testCaseId,
                port,
                load,
                testDuration,
            )

            // Wait for k6 Prometheus Remote Write to flush all metrics
            // k6 pushes metrics in batches (default 5s interval), so we need to wait
            // for the final batch to be written to Prometheus before querying
            logger.info { "Waiting ${PROMETHEUS_FLUSH_DELAY_MS}ms for metrics to be flushed to Prometheus..." }
            Thread.sleep(PROMETHEUS_FLUSH_DELAY_MS)

            val endTime = Instant.now()

            if (result.exitCode != 0) {
                error("k6 test failed with exit code ${result.exitCode}, output:\n${result.output}")
            }

            K6Result(
                startTime = startTime,
                endTime = endTime,
                output = result.output,
                summaryJson = null,
            )
        }
    }

    private fun readSummaryJson(scriptPath: Path): String? {
        val summaryJsonPath = scriptPath.parent.resolve(SUMMARY_FILENAME)
        return try {
            if (summaryJsonPath.toFile().exists()) {
                val content = summaryJsonPath.toFile().readText()
                content
            } else {
                logger.warn { "$SUMMARY_FILENAME not found at ${summaryJsonPath.toFile().absolutePath}" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to read $SUMMARY_FILENAME from ${summaryJsonPath.toFile().absolutePath}" }
            null
        }
    }

    companion object {
        private const val SUMMARY_FILENAME = "summary.json"
        private const val CONTAINER_WORKDIR = "/scripts"

        // Delay to wait for k6 Prometheus Remote Write to flush final metrics batch
        private const val PROMETHEUS_FLUSH_DELAY_MS = 15_000L
    }
}
