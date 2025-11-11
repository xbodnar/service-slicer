package cz.bodnor.serviceslicer.application.module.loadtest.service

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.service.LocalCommandExecutor
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path

@Service
class K6Runner(
    private val localCommandExecutor: LocalCommandExecutor,
    private val k6Properties: K6Properties,
) {

    private val logger = KotlinLogging.logger {}

    data class K6Result(
        val exitCode: Int,
        val output: String,
        val summaryJson: String? = null,
    )

    fun runTest(
        scriptPath: Path,
        environmentVariables: Map<String, String> = emptyMap(),
    ): K6Result {
        logger.info { "Starting k6 load test with script: ${scriptPath.toFile().absolutePath}" }

        val command = buildK6Command(scriptPath, environmentVariables)

        logger.info { "Executing k6 command: ${command.joinToString(" ")}" }

        val result = localCommandExecutor.execute(command, null)

        logger.info { "k6 test completed with exit code: ${result.exitCode}" }

        // Log the full output so you can see what k6 did
        if (result.output.isNotEmpty()) {
            logger.info { "k6 output:\n${result.output}" }
        }

        // Read the summary JSON file that k6 wrote
        val summaryJsonContent = readSummaryJson(scriptPath)

        return K6Result(
            exitCode = result.exitCode,
            output = result.output,
            summaryJson = summaryJsonContent,
        )
    }

    private fun buildK6Command(
        scriptPath: Path,
        environmentVariables: Map<String, String>,
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
        val scriptName = scriptPath.fileName.toString()
        command.addAll(
            listOf(
                "-v",
                "$scriptDir:/scripts",
            ),
        )

        // Add environment variables
        environmentVariables.forEach { (key, value) ->
            command.addAll(listOf("-e", "$key=$value"))
        }

        // Configure Prometheus remote write if enabled
        if (!k6Properties.prometheus.remoteWriteUrl.isNullOrBlank()) {
            command.addAll(
                listOf(
                    "-e",
                    "K6_PROMETHEUS_RW_SERVER_URL=${k6Properties.prometheus.remoteWriteUrl}",
                    "-e",
                    "K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true",
                ),
            )
            logger.debug { "Configured Prometheus remote write to: ${k6Properties.prometheus.remoteWriteUrl}" }
        }

        // Add k6 image and command
        command.addAll(
            listOf(
                k6Properties.dockerImage,
                "run",
            ),
        )

        // Add Prometheus output if configured
        if (!k6Properties.prometheus.remoteWriteUrl.isNullOrBlank()) {
            command.add("--out")
            command.add("experimental-prometheus-rw")
        }

        // Add JSON summary export for structured results
        command.add("--summary-export")
        command.add("/scripts/summary.json")

        // Add script path (inside container)
        command.add("/scripts/$scriptName")

        return command
    }

    private fun readSummaryJson(scriptPath: Path): String? {
        val summaryJsonPath = scriptPath.parent.resolve("summary.json")
        return try {
            if (summaryJsonPath.toFile().exists()) {
                val content = summaryJsonPath.toFile().readText()
                logger.info { "Successfully read summary.json from ${summaryJsonPath.toFile().absolutePath}" }
                content
            } else {
                logger.warn { "summary.json not found at ${summaryJsonPath.toFile().absolutePath}" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to read summary.json from ${summaryJsonPath.toFile().absolutePath}" }
            null
        }
    }
}
