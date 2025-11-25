package cz.bodnor.serviceslicer.application.module.benchmarkrun

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RunSutBenchmarkCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.K6Runner
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.SystemUnderTestRunner
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.writeText

@Component
class RunSutBenchmarkCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: RemoteExecutionProperties,
    private val queryLoadTestMetrics: QueryLoadTestMetrics,
) : CommandHandler<RunSutBenchmarkCommand.LoadTestResult, RunSutBenchmarkCommand> {

    override val command = RunSutBenchmarkCommand::class

    private val logger = KotlinLogging.logger {}

    override fun handle(command: RunSutBenchmarkCommand): RunSutBenchmarkCommand.LoadTestResult {
        logger.info { "Starting benchmark run for SUT ${command.systemUnderTestId}" }

        val benchmark = benchmarkReadService.getById(command.benchmarkId)

        // Start the SUT (blocking call - waits until SUT is healthy and ready)
        sutRunner.startSUT(command.benchmarkId, command.systemUnderTestId)

        var k6WorkDir: Path? = null
        try {
            // Prepare work directory
            k6WorkDir = Files.createTempDirectory("k6-run-")
            val k6ScriptPath = copyK6ScriptToWorkDir(k6WorkDir)
            val configJsonPath = prepareLoadTestConfigFile(benchmark.config, k6WorkDir)

            // Build environment variables for k6
            val envVars = command.buildEnvVars(
                benchmark.getSystemUnderTest(command.systemUnderTestId).dockerConfig.appPort,
            )

            // Run WARMUP k6 tests (no metrics)
            k6Runner.runTest(
                scriptPath = k6ScriptPath,
                configPath = configJsonPath,
                environmentVariables = envVars,
                ignoreMetrics = true,
            )

            // Run k6 test and record start/end times
            val k6Result = k6Runner.runTest(
                scriptPath = k6ScriptPath,
                configPath = configJsonPath,
                environmentVariables = envVars,
            )

            val operationMetrics = queryLoadTestMetrics(
                benchmarkId = command.benchmarkId,
                sutId = command.systemUnderTestId,
                targetVus = command.targetVus,
                start = k6Result.startTime,
                end = k6Result.endTime,
            )

            return RunSutBenchmarkCommand.LoadTestResult(
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

    private fun RunSutBenchmarkCommand.buildEnvVars(appPort: Int): Map<String, String> {
        val sutHost = getSutHost()
        val baseUrl = "http://$sutHost:$appPort"

        return mapOf(
            "BASE_URL" to baseUrl,
            "TARGET_VUS" to targetVus.toString(),
            "DURATION" to k6Properties.testDuration,
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
