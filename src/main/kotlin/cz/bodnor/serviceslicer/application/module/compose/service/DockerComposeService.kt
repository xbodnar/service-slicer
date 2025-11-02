package cz.bodnor.serviceslicer.application.module.compose.service

import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Service
class DockerComposeService {

    private val restClient = RestClient.builder().baseUrl("http://localhost:9090").build()

    private val clock = Clock.systemUTC()

    private val retryInterval = 5.seconds

    private val log = logger()

    fun runAndValidate(
        composeFilePath: Path,
        healthCheckUrl: String,
        startupTimeoutSeconds: Int,
    ): Boolean {
        val composeDir = composeFilePath.parent
        val composeFileName = composeFilePath.fileName.toString()

        try {
            // Start Docker Compose
            log.info("Starting Docker Compose from ${composeFilePath.toAbsolutePath()}")
            executeCommand(
                directory = composeDir,
                command = listOf("docker", "compose", "-f", composeFileName, "up", "-d"),
            )

            val startTime = clock.instant().epochSecond
            var isHealthy = false
            // Try health until timeout is reached
            do {
                Thread.sleep(retryInterval.inWholeMilliseconds)

                val timeElapsed = clock.instant().epochSecond - startTime

                log.info("Checking health at $healthCheckUrl after $timeElapsed seconds")
                isHealthy = try {
                    val response = restClient.get()
                        .uri(healthCheckUrl)
                        .retrieve()
                        .toBodilessEntity()
                    response.statusCode.is2xxSuccessful
                } catch (e: Exception) {
                    false
                }
                log.info("Health check result: $isHealthy")
            } while (!isHealthy && timeElapsed < startupTimeoutSeconds)

            return isHealthy
        } finally {
            // Always stop the containers
            log.info("Stopping Docker Compose")
            executeCommand(
                directory = composeDir,
                command = listOf("docker", "compose", "-f", composeFileName, "down"),
            )
        }
    }

    private fun executeCommand(
        directory: Path,
        command: List<String>,
    ) {
        val process = ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val exitCode = process.waitFor(60, TimeUnit.SECONDS)
        if (!exitCode || process.exitValue() != 0) {
            val error = process.errorStream.bufferedReader().readText()
            val output = process.inputStream.bufferedReader().readText()
            log.error("Command failed: ${command.joinToString(" ")}\nOutput: $output\nError: $error")
            throw RuntimeException("Docker command failed: ${command.joinToString(" ")}")
        }
    }
}
