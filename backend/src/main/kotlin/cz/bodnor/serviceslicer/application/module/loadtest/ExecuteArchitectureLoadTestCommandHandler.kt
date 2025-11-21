package cz.bodnor.serviceslicer.application.module.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.loadtest.command.ExecuteArchitectureLoadTestCommand
import cz.bodnor.serviceslicer.application.module.loadtest.port.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.application.module.loadtest.service.K6Runner
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.service.SystemUnderTestRunner
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.domain.loadtestrun.LoadTestRunRepository
import cz.bodnor.serviceslicer.domain.loadtestrun.SutLoadTestRun
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.writeText

@Component
class ExecuteArchitectureLoadTestCommandHandler(
    private val loadTestConfigReadService: LoadTestConfigReadService,
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: RemoteExecutionProperties,
    private val queryLoadTestMetrics: QueryLoadTestMetrics,
    private val loadTestRunRepository: LoadTestRunRepository,
) : CommandHandler<Unit, ExecuteArchitectureLoadTestCommand> {

    override val command = ExecuteArchitectureLoadTestCommand::class

    private val logger = KotlinLogging.logger {}

    override fun handle(command: ExecuteArchitectureLoadTestCommand) {
        logger.info { "Starting load test execution for SUT ${command.systemUnderTestId}" }

        // Start the SUT (blocking call - waits until SUT is healthy and ready)
        sutRunner.startSUT(command.systemUnderTestId)

        var k6WorkDir: Path? = null
        try {
            // Prepare work directory
            k6WorkDir = Files.createTempDirectory("k6-run-")
            val k6ScriptPath = copyK6ScriptToWorkDir(k6WorkDir)
            val configJsonPath = prepareLoadTestConfigFile(command.experimentId, k6WorkDir)

            // Build environment variables for k6
            val envVars = command.buildEnvVars()

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

            // Fetch prometheus metrics
            saveRunMetrics(command, k6Result)
        } finally {
            logger.info { "Load test execution finished, cleaning up..." }
            sutRunner.stopSUT()
            k6WorkDir?.toFile()?.deleteRecursively()
        }
    }

    private fun saveRunMetrics(
        command: ExecuteArchitectureLoadTestCommand,
        k6Result: K6Runner.K6Result,
    ) {
        val operationMetrics = queryLoadTestMetrics(
            experimentId = command.experimentId,
            sutId = command.systemUnderTestId,
            targetVus = command.targetVus,
            start = k6Result.startTime,
            end = k6Result.endTime,
        )

        // 10. Persist LoadTestRun to database
        val sutLoadTestRun = SutLoadTestRun(
            experimentId = command.experimentId,
            systemUnderTestId = command.systemUnderTestId,
            targetVus = command.targetVus,
            startTimestamp = k6Result.startTime,
            endTimestamp = k6Result.endTime,
            operationMeasurements = operationMetrics,
        )
        loadTestRunRepository.save(sutLoadTestRun)
    }

    private fun ExecuteArchitectureLoadTestCommand.buildEnvVars(): Map<String, String> {
        val sut = loadTestExperimentReadService.getSystemUnderTestById(experimentId, systemUnderTestId)

        val sutHost = getSutHost()
        val baseUrl = "http://$sutHost:${sut.dockerConfig.appPort}"

        return mapOf(
            "BASE_URL" to baseUrl,
            "TARGET_VUS" to targetVus.toString(),
            "DURATION" to k6Properties.testDuration,
            "EXPERIMENT_ID" to experimentId.toString(),
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
        experimentId: UUID,
        workDir: Path,
    ): Path {
        val experiment = loadTestExperimentReadService.getById(experimentId)
        val loadTestConfig = loadTestConfigReadService.getById(experiment.loadTestConfigId)

        val configFile = workDir.resolve("load-test-config.json")
        configFile.writeText(objectMapper.writeValueAsString(loadTestConfig))

        logger.info { "Prepared load test config file at: ${configFile.toFile().absolutePath}" }

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
            Files.copy(input, scriptFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }

        logger.info { "Copied k6 script to: ${scriptFile.toFile().absolutePath}" }

        return scriptFile
    }
}
