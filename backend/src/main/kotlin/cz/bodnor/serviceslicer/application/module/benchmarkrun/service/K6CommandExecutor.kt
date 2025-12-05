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
) {

    fun executeValidation(
        operationalSettingId: UUID,
        sutPort: Int,
    ): CommandExecutor.CommandResult {
        // When running in Docker, use container network to reach SSH tunnel
        val (networkCommandOption, networkCommandValue) = if (k6Properties.dockerNetworkConfig != null) {
            "--network" to k6Properties.dockerNetworkConfig.networkName
        } else {
            // When running natively, add host entry to reach SSH tunnel
            "--add-host" to "host.docker.internal:host-gateway"
        }

        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
            networkCommandOption, networkCommandValue,
            "-e", "BASE_URL=${getBaseUrl(sutPort)}",
            "-e", "CONFIG_URL=${getConfigUrl(operationalSettingId)}",
            k6Properties.dockerImage,
            "run",
            ScriptFile.VALIDATION.file.toString(),
        )

        return localCommandExecutor.execute(command, null)
    }

    fun runK6WarmUp(
        operationalSettingId: UUID,
        testCaseId: UUID,
        sutPort: Int,
        load: Int,
    ): CommandExecutor.CommandResult {
        // When running in Docker, use container network to reach SSH tunnel
        val (networkCommandOption, networkCommandValue) = if (k6Properties.dockerNetworkConfig != null) {
            "--network" to k6Properties.dockerNetworkConfig.networkName
        } else {
            // When running natively, add host entry to reach SSH tunnel
            "--add-host" to "host.docker.internal:host-gateway"
        }

        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
            networkCommandOption, networkCommandValue,
            "-e", "BASE_URL=${getBaseUrl(sutPort)}",
            "-e", "CONFIG_URL=${getConfigUrl(operationalSettingId)}",
            "-e", "TEST_CASE_ID=$testCaseId",
            "-e", "TARGET_VUS=$load",
            "-e", "DURATION=30s",
            k6Properties.dockerImage,
            "run",
            ScriptFile.EXPERIMENT.file.toString(),
        )

        return localCommandExecutor.execute(command, null)
    }

    fun runK6Test(
        operationalSettingId: UUID,
        testCaseId: UUID,
        sutPort: Int,
        load: Int,
        duration: String,
    ): CommandExecutor.CommandResult {
        // When running in Docker, use container network to reach SSH tunnel
        val (networkCommandOption, networkCommandValue) = if (k6Properties.dockerNetworkConfig != null) {
            "--network" to k6Properties.dockerNetworkConfig.networkName
        } else {
            // When running natively, add host entry to reach SSH tunnel
            "--add-host" to "host.docker.internal:host-gateway"
        }

        val command = mutableListOf(
            "docker",
            "run",
            "--rm",
            networkCommandOption, networkCommandValue,
            "-e", "BASE_URL=${getBaseUrl(sutPort)}",
            "-e", "CONFIG_URL=${getConfigUrl(operationalSettingId)}",
            "-e", "TEST_CASE_ID=$testCaseId",
            "-e", "TARGET_VUS=$load",
            "-e", "DURATION=$duration",
            "-e", "K6_PROMETHEUS_RW_SERVER_URL=${prometheusProperties.remoteWriteUrl}",
            "-e", "K6_PROMETHEUS_RW_TREND_AS_NATIVE_HISTOGRAM=true",
            k6Properties.dockerImage,
            "run",
            "--out", "experimental-prometheus-rw",
            "--summary-export", "/scripts/summary.json",
            ScriptFile.EXPERIMENT.file.toString(),
        )

        return localCommandExecutor.execute(command, null)
    }

    private fun getBaseUrl(appPort: Int): String {
        val sutHost = k6Properties.dockerNetworkConfig?.containerName ?: "host.docker.internal"

        return "http://$sutHost:$appPort"
    }

    private fun getConfigUrl(operationalSettingId: UUID): String {
        val host = k6Properties.dockerNetworkConfig?.containerName ?: "host.docker.internal"
        return "http://$host:8080/api/operational-settings/$operationalSettingId"
    }
}

enum class ScriptFile(
    val file: Path,
) {
    VALIDATION(Path.of("/scripts/validation-template.js")),
    EXPERIMENT(Path.of("/scripts/experiment-template.js")),
}
