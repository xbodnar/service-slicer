package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.PrometheusProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.time.Instant

@Service
class K6Runner(
    private val localCommandExecutor: LocalCommandExecutor,
    private val k6Properties: K6Properties,
    private val prometheusProperties: PrometheusProperties,
    private val objectMapper: ObjectMapper,
) {

    private val logger = KotlinLogging.logger {}

    data class K6Result(
        val startTime: Instant,
        val endTime: Instant,
        val output: String,
        val summaryJson: JsonNode? = null,
    )

    fun runTest(
        scriptPath: Path,
        operationalSettingPath: Path,
        environmentVariables: Map<String, String> = emptyMap(),
        ignoreMetrics: Boolean = false,
    ): K6Result {
        logger.info { "Executing k6 ${if (ignoreMetrics) "warmup run..." else "test run..."}" }
        val command = buildK6Command(scriptPath, operationalSettingPath, environmentVariables, ignoreMetrics)

        logger.debug { "Executing k6 command: ${command.joinToString(" ")}" }

        val startTime = Instant.now()
        val result = localCommandExecutor.execute(command, null)
        val endTime = Instant.now()

        logger.info { "k6 test completed with exit code: ${result.exitCode}" }
        if (result.exitCode != 0) {
            error("k6 test failed with exit code ${result.exitCode}, output:\n${result.output}")
        }

        // Read the summary JSON file that k6 wrote
        val summaryJsonContent = readSummaryJson(scriptPath)

        return K6Result(
            startTime = startTime,
            endTime = endTime,
            output = result.output,
            summaryJson = objectMapper.readTree(summaryJsonContent),
        )
    }

    private fun buildK6Command(
        scriptPath: Path,
        operationalSettingPath: Path,
        environmentVariables: Map<String, String>,
        ignoreMetrics: Boolean,
    ): List<String> {
        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
            // Add host.docker.internal mapping for Mac/Windows to reach host services
            "--add-host",
            "host.docker.internal:host-gateway",
        )

        // Mount script directory (read-write so k6 can write summary.json)
        val scriptDir = scriptPath.parent.toFile().absolutePath
        command.addAll(
            listOf(
                "-v",
                "$scriptDir:$CONTAINER_WORKDIR",
            ),
        )

        command.addAll(listOf("-e", "OPERATIONAL_SETTING_FILE=$CONTAINER_WORKDIR/${operationalSettingPath.fileName}"))

        // Add environment variables
        environmentVariables.forEach { (key, value) ->
            command.addAll(listOf("-e", "$key=$value"))
        }

        // Configure Prometheus remote write if enabled
        if (!ignoreMetrics && !prometheusProperties.remoteWriteUrl.isNullOrBlank()) {
            command.addAll(
                listOf(
                    "-e",
                    "K6_PROMETHEUS_RW_SERVER_URL=${prometheusProperties.remoteWriteUrl}",
                    "-e",
                    "K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true",
                ),
            )
            logger.debug { "Configured Prometheus remote write to: ${prometheusProperties.remoteWriteUrl}" }
        }

        // Add k6 image and command
        command.addAll(
            listOf(
                k6Properties.dockerImage,
                "run",
            ),
        )

        // Add Prometheus output if configured
        if (!ignoreMetrics && !prometheusProperties.remoteWriteUrl.isNullOrBlank()) {
            command.add("--out")
            command.add("experimental-prometheus-rw")
        }

        // Add JSON summary export for structured results
        command.add("--summary-export")
        command.add("$CONTAINER_WORKDIR/$SUMMARY_FILENAME")

        // Add script path (inside container)
        command.add("$CONTAINER_WORKDIR/${scriptPath.fileName}")

        return command
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
