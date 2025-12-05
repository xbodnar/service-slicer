package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.domain.benchmark.ValidationState
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.io.path.writeText

@Service
class ValidationRunner(
    private val benchmarkReadService: BenchmarkReadService,
    private val sutReadService: SystemUnderTestReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: RemoteExecutionProperties,
) {

    private val logger = KotlinLogging.logger {}

    fun runSutValidation(
        benchmarkId: UUID,
        systemUnderTestId: UUID,
    ): ValidationResult {
        logger.info { "Starting validation run for benchmark $benchmarkId and SUT $systemUnderTestId" }

        val benchmark = benchmarkReadService.getById(benchmarkId)
        val sut = sutReadService.getById(systemUnderTestId)

        var k6WorkDir: Path? = null
        try {
            // Start the SUT (blocking call - waits until SUT is healthy and ready)
            sutRunner.startSUT(sut)

            // Prepare work directory in the shared k6 scripts location
            k6WorkDir = Files.createTempDirectory("k6-validation-")
            val k6ScriptPath = copyValidationScriptToWorkDir(k6WorkDir)
            val configJsonPath = prepareLoadTestConfigFile(benchmark.operationalSetting, k6WorkDir)

            // Build environment variables for k6
            val envVars = buildEnvVars(
                benchmarkId = benchmarkId,
                systemUnderTestId = systemUnderTestId,
                appPort = sut.dockerConfig.appPort,
            )

            // Run validation test (no metrics, single iteration through all behavior models)
            val k6Result = k6Runner.runTest(
                scriptPath = k6ScriptPath,
                operationalSettingPath = configJsonPath,
                environmentVariables = envVars,
                ignoreMetrics = true,
            )

            return ValidationResult(
                validationState = ValidationState.VALID,
                k6Output = k6Result.output,
            )
        } catch (e: Exception) {
            logger.error(e) { "Validation run failed for benchmark $benchmarkId and SUT $systemUnderTestId" }
            return ValidationResult(
                validationState = ValidationState.INVALID,
                errorMessage = e.message ?: "Unknown error during validation",
            )
        } finally {
            logger.info { "Validation run finished, cleaning up..." }
            sutRunner.stopSUT()
            k6WorkDir?.toFile()?.deleteRecursively()
        }
    }

    private fun buildEnvVars(
        benchmarkId: UUID,
        systemUnderTestId: UUID,
        appPort: Int,
    ): Map<String, String> {
        val sutHost = getSutHost()
        val baseUrl = "http://$sutHost:$appPort"

        return mapOf(
            "BASE_URL" to baseUrl,
            "BENCHMARK_ID" to benchmarkId.toString(),
            "SUT_ID" to systemUnderTestId.toString(),
        )
    }

    // K6 runs in a Docker container:
    // - If SUT is deployed locally, use host.docker.internal to reach host
    // - If SUT is deployed remotely, use the remote host's IP directly
    private fun getSutHost(): String = if (remoteExecutionProperties.enabled) {
        sutRunner.commandExecutor.getTargetHost()
    } else {
        "host.docker.internal"
    }

    private fun prepareLoadTestConfigFile(
        operationalSetting: OperationalSetting,
        workDir: Path,
    ): Path {
        val configFile = workDir.resolve("benchmark-config.json")
        configFile.writeText(objectMapper.writeValueAsString(operationalSetting))

        return configFile
    }

    private fun copyValidationScriptToWorkDir(workDir: Path): Path {
        // Get the k6 validation script from resources
        val resource = resourceLoader.getResource("classpath:k6/validation-template.js")
        if (!resource.exists()) {
            throw IllegalStateException("k6 validation script not found: k6/validation-template.js")
        }

        // Copy script to the work directory
        val scriptFile = workDir.resolve("validation-template.js")
        resource.inputStream.use { input ->
            Files.copy(input, scriptFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return scriptFile
    }
}
