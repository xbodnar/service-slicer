package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.OperationMetrics
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.util.UUID
import kotlin.io.path.writeText

@Service
class TestCaseRunner(
    private val benchmarkReadService: BenchmarkReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: RemoteExecutionProperties,
    private val queryLoadTestMetrics: QueryLoadTestMetrics,
) {

    private val logger = KotlinLogging.logger {}

    data class Input(
        val benchmarkId: UUID,
        val benchmarkRunId: UUID,
        val sutId: UUID,
        val load: Int,
    )

    data class Result(
        val startTimestamp: Instant,
        val endTimestamp: Instant,
        val operationMeasurements: List<OperationMetrics>,
        val k6Output: String,
    )

    fun runTestCase(input: Input): Result {
        logger.info { "Starting benchmark run for SUT ${input.sutId} with load ${input.load}" }

        val benchmark = benchmarkReadService.getById(input.benchmarkId)

        // Start the SUT (blocking call - waits until SUT is healthy and ready)
        sutRunner.startSUT(input.sutId)

        var k6WorkDir: Path? = null
        try {
            // Prepare work directory
            k6WorkDir = Files.createTempDirectory("k6-run-")
            val k6ScriptPath = copyK6ScriptToWorkDir(k6WorkDir)
            val configJsonPath = prepareLoadTestConfigFile(benchmark.config, k6WorkDir)

            // Build environment variables for k6
            val envVars = input.buildEnvVars(
                benchmark.getSystemUnderTest(input.sutId).dockerConfig.appPort,
            )

            // Run WARMUP k6 tests (no metrics)
            k6Runner.runTest(
                scriptPath = k6ScriptPath,
                configPath = configJsonPath,
                environmentVariables = envVars + mapOf("DURATION" to "30s"),
                ignoreMetrics = true,
            )

            // Run k6 test and record start/end times
            val k6Result = k6Runner.runTest(
                scriptPath = k6ScriptPath,
                configPath = configJsonPath,
                environmentVariables = envVars,
            )

            val operationMetrics = queryLoadTestMetrics(
                benchmarkId = input.benchmarkId,
                sutId = input.sutId,
                targetVus = input.load,
                start = k6Result.startTime,
                end = k6Result.endTime,
            )

            return Result(
                startTimestamp = k6Result.startTime,
                endTimestamp = k6Result.endTime,
                operationMeasurements = operationMetrics,
                k6Output = k6Result.output,
            )
        } finally {
            logger.info { "Load test execution finished, cleaning up..." }
            sutRunner.stopSUT()
            k6WorkDir?.toFile()?.deleteRecursively()
        }
    }

    private fun Input.buildEnvVars(appPort: Int): Map<String, String> {
        val sutHost = getSutHost()
        val baseUrl = "http://$sutHost:$appPort"

        return mapOf(
            "BASE_URL" to baseUrl,
            "TARGET_VUS" to load.toString(),
            "DURATION" to k6Properties.testDuration,
            "BENCHMARK_ID" to benchmarkId.toString(),
            "BENCHMARK_RUN_ID" to benchmarkRunId.toString(),
            "SUT_ID" to sutId.toString(),
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
        benchmarkConfig: BenchmarkConfig,
        workDir: Path,
    ): Path {
        val configFile = workDir.resolve("benchmark-config.json")
        configFile.writeText(objectMapper.writeValueAsString(benchmarkConfig))

        return configFile
    }

    private fun copyK6ScriptToWorkDir(workDir: Path): Path {
        // Get the k6 test script from resources
        val resource = resourceLoader.getResource("classpath:k6/experiment-template.js")
        if (!resource.exists()) {
            throw IllegalStateException("k6 test script not found: k6/experiment-template.js")
        }

        // Copy script to the work directory
        val scriptFile = workDir.resolve("experiment-template.js")
        resource.inputStream.use { input ->
            Files.copy(input, scriptFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return scriptFile
    }
}
