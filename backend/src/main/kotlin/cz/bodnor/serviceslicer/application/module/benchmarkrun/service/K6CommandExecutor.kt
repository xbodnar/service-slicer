package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.PrometheusProperties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.UUID

@Component
class K6CommandExecutor(
    private val k6Properties: K6Properties,
    private val localCommandExecutor: LocalCommandExecutor,
    private val prometheusProperties: PrometheusProperties,
    private val remoteExecutionProperties: RemoteExecutionProperties,
) {
    private val isRunningInDocker: Boolean by lazy {
        System.getenv("SPRING_PROFILES_ACTIVE")?.contains("demo") == true ||
            java.io.File("/.dockerenv").exists()
    }

    private val containerHostname: String by lazy {
        if (isRunningInDocker) {
            System.getenv("HOSTNAME") ?: "localhost"
        } else {
            "localhost"
        }
    }

    fun executeValidation(
        operationalSettingId: UUID,
        appPort: Int,
        tunnelLocalPort: Int? = null,
    ): CommandExecutor.CommandResult {
        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
        )

        // When running in Docker, use container network to reach SSH tunnel
        if (isRunningInDocker) {
            command.addAll(listOf("--network", "container:$containerHostname"))
        } else {
            command.addAll(listOf("--add-host", "host.docker.internal:host-gateway"))
        }

        command.addAll(
            listOf(
                "-e", "BASE_URL=${getBaseUrl(appPort, tunnelLocalPort)}",
                "-e", "CONFIG_URL=${getConfigUrl(operationalSettingId)}",
                k6Properties.dockerImage,
                "run",
                ScriptFile.VALIDATION.file.toString(),
            ),
        )

        return localCommandExecutor.execute(command, null)
    }

    fun runK6WarmUp(
        operationalSettingId: UUID,
        testCaseId: UUID,
        appPort: Int,
        load: Int,
        tunnelLocalPort: Int? = null,
    ): CommandExecutor.CommandResult {
        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
        )

        if (isRunningInDocker) {
            command.addAll(listOf("--network", "container:$containerHostname"))
        } else {
            command.addAll(listOf("--add-host", "host.docker.internal:host-gateway"))
        }

        command.addAll(
            listOf(
                "-e", "BASE_URL=${getBaseUrl(appPort, tunnelLocalPort)}",
                "-e", "CONFIG_URL=${getConfigUrl(operationalSettingId)}",
                "-e", "TEST_CASE_ID=$testCaseId",
                "-e", "TARGET_VUS=$load",
                "-e", "DURATION=30s",
                k6Properties.dockerImage,
                "run",
                ScriptFile.EXPERIMENT.file.toString(),
            ),
        )

        return localCommandExecutor.execute(command, null)
    }

    fun runK6Test(
        operationalSettingId: UUID,
        testCaseId: UUID,
        appPort: Int,
        load: Int,
        duration: String,
        tunnelLocalPort: Int? = null,
    ): CommandExecutor.CommandResult {
        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
        )

        if (isRunningInDocker) {
            command.addAll(listOf("--network", "container:$containerHostname"))
        } else {
            command.addAll(listOf("--add-host", "host.docker.internal:host-gateway"))
        }

        command.addAll(
            listOf(
                "-e", "BASE_URL=${getBaseUrl(appPort, tunnelLocalPort)}",
                "-e", "CONFIG_URL=${getConfigUrl(operationalSettingId)}",
                "-e", "TEST_CASE_ID=$testCaseId",
                "-e", "TARGET_VUS=$load",
                "-e", "DURATION=$duration",
                k6Properties.dockerImage,
                "run",
                ScriptFile.EXPERIMENT.file.toString(),
            ),
        )

        return localCommandExecutor.execute(command, null)
    }

    private fun getBaseUrl(
        appPort: Int,
        tunnelLocalPort: Int?,
    ): String {
        val sutHost = if (isRunningInDocker) "localhost" else "host.docker.internal"
        val port = tunnelLocalPort ?: appPort

        return "http://$sutHost:$port"
    }

    private fun getConfigUrl(operationalSettingId: UUID): String {
        val host = if (isRunningInDocker) "localhost" else "host.docker.internal"
        return "http://$host:8080/api/operational-settings/$operationalSettingId"
    }
}

enum class ScriptFile(
    val file: Path,
) {
    VALIDATION(Path.of("/scripts/validation-template.js")),
    EXPERIMENT(Path.of("/scripts/experiment-template.js")),
}
