package cz.bodnor.serviceslicer.application.module.loadtest

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.loadtest.command.ExecuteLoadTestCommand
import cz.bodnor.serviceslicer.application.module.loadtest.service.K6Runner
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.service.SystemUnderTestRunner
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfig
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

@Component
class ExecuteLoadTestCommandHandler(
    private val loadTestConfigReadService: LoadTestConfigReadService,
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties,
) : CommandHandler<ExecuteLoadTestCommand.Result, ExecuteLoadTestCommand> {

    override val command = ExecuteLoadTestCommand::class

    private val logger = KotlinLogging.logger {}

    override fun handle(command: ExecuteLoadTestCommand): ExecuteLoadTestCommand.Result {
        logger.info { "Starting load test execution for SUT ${command.systemUnderTestId}" }

        // 1. Fetch SUT and LoadTestConfig from database
        val experiment = loadTestExperimentReadService.getById(command.experimentId)

        val sut = experiment.systemsUnderTest.find { it.id == command.systemUnderTestId }
            ?: error("SUT with id: ${command.systemUnderTestId} not found for experiment: ${experiment.id}")
        val loadTestConfig = loadTestConfigReadService.getById(experiment.loadTestConfigId)

        logger.info { "SUT: ${sut.name}, LoadTestConfig ID: ${loadTestConfig.id}" }

        // 2. Start the SUT (blocking call - waits until SUT is healthy and ready)
        logger.info { "Starting SUT..." }
        sutRunner.startSUT(command.systemUnderTestId)
        logger.info { "SUT is ready!" }

        var k6WorkDir: Path? = null
        try {
            // 3. Create a dedicated temporary directory for this k6 run
            k6WorkDir = Files.createTempDirectory("k6-run-")
            logger.info { "Created k6 work directory: ${k6WorkDir.toFile().absolutePath}" }

            // 4. Copy k6 script to the work directory
            val k6ScriptPath = copyK6ScriptToWorkDir(k6WorkDir)

            // 5. Create load test configuration JSON file in the work directory
            val configJsonPath = prepareLoadTestConfigFile(loadTestConfig, k6WorkDir)

            // 6. Build environment variables for k6
            // K6 runs in a local Docker container:
            // - If SUT is deployed locally, use host.docker.internal to reach host
            // - If SUT is deployed remotely, use the remote host's IP directly
            val sutHost = if (remoteExecutionProperties.enabled) {
                sutRunner.commandExecutor.getTargetHost()
            } else {
                "host.docker.internal"
            }
            val baseUrl = "http://$sutHost:${sut.appPort}"
            val envVars = mapOf(
                "BASE_URL" to baseUrl,
                "TARGET_VUS" to command.targetVus.toString(),
                "DURATION" to k6Properties.testDuration,
                "EXPERIMENT_ID" to command.experimentId.toString(),
                "SUT_ID" to command.systemUnderTestId.toString(),
            )

            logger.info {
                "Running k6 test with BASE_URL=$baseUrl, TARGET_VUS=${command.targetVus}, DURATION=${k6Properties.testDuration}"
            }

            // 7. Run k6 test
            val k6Result = k6Runner.runTest(
                scriptPath = k6ScriptPath,
                configPath = configJsonPath,
                environmentVariables = envVars,
            )

            logger.info { "k6 test completed with exit code: ${k6Result.exitCode}" }

            // 8. Return results
            return ExecuteLoadTestCommand.Result(
                summaryJson = k6Result.summaryJson ?: "",
                exitCode = k6Result.exitCode,
                stdOut = k6Result.output,
            )
        } finally {
            logger.info { "Load test execution completed. Stopping SUT..." }
            sutRunner.stopSUT()
            logger.info { "Load test execution completed. Cleaning up temp files..." }
            k6WorkDir?.toFile()?.deleteRecursively()
        }
    }

    private fun prepareLoadTestConfigFile(
        loadTestConfig: LoadTestConfig,
        workDir: Path,
    ): Path {
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
