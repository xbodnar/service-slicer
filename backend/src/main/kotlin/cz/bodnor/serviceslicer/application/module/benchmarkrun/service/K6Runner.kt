package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.PrometheusProperties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

@Service
class K6Runner(
    private val k6Properties: K6Properties,
    private val k6CommandExecutor: K6CommandExecutor,
    private val prometheusProperties: PrometheusProperties,
    private val objectMapper: ObjectMapper,
    private val remoteExecutionProperties: RemoteExecutionProperties,
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

        if (remoteExecutionProperties.enabled) {
            sshTunnelManager.openTunnel(appPort).use { tunnel ->
                logger.info {
                    "Using SSH tunnel for validation: localhost:${tunnel.localPort} -> ${remoteExecutionProperties.host}:$appPort"
                }

                val startTime = Instant.now()
                val result = k6CommandExecutor.executeValidation(operationalSettingId, appPort, tunnel.localPort)
                val endTime = Instant.now()

                if (result.exitCode != 0) {
                    error("k6 validation failed with exit code ${result.exitCode}, output:\n${result.output}")
                }

                return K6Result(
                    startTime = startTime,
                    endTime = endTime,
                    output = result.output,
                    summaryJson = null,
                )
            }
        } else {
            val startTime = Instant.now()
            val result = k6CommandExecutor.executeValidation(operationalSettingId, appPort)
            val endTime = Instant.now()

            if (result.exitCode != 0) {
                error("k6 validation failed with exit code ${result.exitCode}, output:\n${result.output}")
            }

            return K6Result(
                startTime = startTime,
                endTime = endTime,
                output = result.output,
                summaryJson = null,
            )
        }
    }

    fun runTest(
        operationalSettingId: UUID,
        testCaseId: UUID,
        appPort: Int,
        load: Int,
        testDuration: String,
    ): K6Result {
        logger.info { "Executing k6 test run..." }

        if (remoteExecutionProperties.enabled) {
            sshTunnelManager.openTunnel(appPort).use { tunnel ->
                logger.info {
                    "Using SSH tunnel for test: localhost:${tunnel.localPort} -> ${remoteExecutionProperties.host}:$appPort"
                }

                // Warmup run
                k6CommandExecutor.runK6WarmUp(operationalSettingId, testCaseId, appPort, load, tunnel.localPort)

                // Main run
                val startTime = Instant.now()
                val result = k6CommandExecutor.runK6Test(
                    operationalSettingId,
                    testCaseId,
                    appPort,
                    load,
                    testDuration,
                    tunnel.localPort,
                )
                val endTime = Instant.now()

                if (result.exitCode != 0) {
                    error("k6 test failed with exit code ${result.exitCode}, output:\n${result.output}")
                }

                return K6Result(
                    startTime = startTime,
                    endTime = endTime,
                    output = result.output,
                    summaryJson = null,
                )
            }
        } else {
            // Warmup run
            k6CommandExecutor.runK6WarmUp(operationalSettingId, testCaseId, appPort, load)

            // Main run
            val startTime = Instant.now()
            val result = k6CommandExecutor.runK6Test(operationalSettingId, testCaseId, appPort, load, testDuration)
            val endTime = Instant.now()

            if (result.exitCode != 0) {
                error("k6 test failed with exit code ${result.exitCode}, output:\n${result.output}")
            }

            return K6Result(
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
    }
}
