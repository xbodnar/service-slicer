package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.domain.testcase.TestCase
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.time.Instant

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

    fun runTestCase(testCase: TestCase): Result {
        val load = testCase.load
        val sut = testCase.testSuite.systemUnderTest
        val duration = testCase.testSuite.benchmarkRun.testDuration

        logger.info {
            "Executing TestCase for SUT ${sut.id} [${sut.name}] with load=$load and duration=$duration"
        }
        // Start the SUT (blocking call - waits until SUT is healthy and ready)
        sutRunner.startSUT(sut)

        try {
            val k6Result = k6Runner.runTest(
                benchmarkRunId = testCase.testSuite.benchmarkRun.id,
                testCaseId = testCase.id,
                appPort = sut.dockerConfig.appPort,
                load = load,
                testDuration = duration.toString(),
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
        }
    }
}
