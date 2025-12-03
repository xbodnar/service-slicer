package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.domain.testcase.TestCase
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
    private val sutReadService: SystemUnderTestReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: RemoteExecutionProperties,
    private val queryLoadTestMetrics: QueryLoadTestMetrics,
) {

    private val logger = KotlinLogging.logger {}

    data class Result(
        val startTimestamp: Instant,
        val endTimestamp: Instant,
        val performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        val k6Output: String,
        val jsonSummary: JsonNode?,
    )

    fun runTestCase(
        benchmarkRun: BenchmarkRun,
        testCase: TestCase,
    ): Result {
        val benchmark = benchmarkRun.benchmark
        val (sut, load) = if (testCase.id == benchmarkRun.baselineTestCase.id) {
            benchmark.baselineSut to benchmark.operationalSetting.operationalProfile.keys.min()
        } else {
            val testCase = benchmarkRun.targetTestCases.find { it.id == testCase.id }
                ?: error("No test case with id ${testCase.id}")
            benchmark.targetSut to testCase.load
        }

        logger.info {
            "Executing ${if (testCase.id == benchmarkRun.baselineTestCase.id) "Baseline" else "Target"}" +
                "TestCase for SUT ${sut.id} [${sut.name}] with load=$load and duration=${benchmarkRun.testDuration}"
        }
        // Start the SUT (blocking call - waits until SUT is healthy and ready)
        sutRunner.startSUT(sut)

        var k6WorkDir: Path? = null
        try {
            // Prepare work directory
            k6WorkDir = Files.createTempDirectory("k6-run-")
            val k6ScriptPath = copyK6ScriptToWorkDir(k6WorkDir)
            val configJsonPath = prepareOperationalSettingFile(benchmark.operationalSetting, k6WorkDir)

            // Build environment variables for k6
            val envVars =
                buildEnvVars(
                    appPort = sut.dockerConfig.appPort,
                    load = load,
                    testCaseId = testCase.id,
                    testDuration = benchmarkRun.testDuration.toString(),
                )

            // Run WARMUP k6 tests (no metrics)
            k6Runner.runTest(
                scriptPath = k6ScriptPath,
                operationalSettingPath = configJsonPath,
                environmentVariables = envVars + mapOf("DURATION" to "30s"),
                ignoreMetrics = true,
            )

            // Run k6 test and record start/end times
            val k6Result = k6Runner.runTest(
                scriptPath = k6ScriptPath,
                operationalSettingPath = configJsonPath,
                environmentVariables = envVars,
            )

            val performanceMetrics = queryLoadTestMetrics(
                testCaseId = testCase.id,
                start = k6Result.startTime,
                end = k6Result.endTime,
            )

            return Result(
                startTimestamp = k6Result.startTime,
                endTimestamp = k6Result.endTime,
                performanceMetrics = performanceMetrics,
                k6Output = k6Result.output,
                jsonSummary = k6Result.summaryJson,
            )
        } finally {
            logger.info { "Test Case execution finished, cleaning up..." }
            sutRunner.stopSUT()
            k6WorkDir?.toFile()?.deleteRecursively()
        }
    }

    private fun buildEnvVars(
        appPort: Int,
        load: Int,
        testCaseId: UUID,
        testDuration: String,
    ): Map<String, String> {
        val sutHost = getSutHost()
        val baseUrl = "http://$sutHost:$appPort"

        return mapOf(
            "BASE_URL" to baseUrl,
            "TARGET_VUS" to load.toString(),
            "DURATION" to testDuration,
            "TEST_CASE_ID" to testCaseId.toString(),
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

    private fun prepareOperationalSettingFile(
        operationalSetting: OperationalSetting,
        workDir: Path,
    ): Path {
        val configFile = workDir.resolve("operational-setting.json")
        configFile.writeText(objectMapper.writeValueAsString(operationalSetting))

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
