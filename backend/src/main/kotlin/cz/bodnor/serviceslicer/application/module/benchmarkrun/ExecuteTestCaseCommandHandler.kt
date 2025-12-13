package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ExecuteTestCaseCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.TestCaseRunner
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.testcase.TestCase
import cz.bodnor.serviceslicer.domain.testcase.TestCaseReadService
import cz.bodnor.serviceslicer.domain.testcase.TestCaseWriteService
import cz.bodnor.serviceslicer.domain.testsuite.TestSuiteWriteService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ExecuteTestCaseCommandHandler(
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
    private val testCaseReadService: TestCaseReadService,
    private val testCaseWriteService: TestCaseWriteService,
    private val testSuiteWriteService: TestSuiteWriteService,
    private val testCaseRunner: TestCaseRunner,
) : CommandHandler<ExecuteTestCaseCommand.Result, ExecuteTestCaseCommand> {

    override val command = ExecuteTestCaseCommand::class

    @Lazy
    @Autowired
    private lateinit var self: ExecuteTestCaseCommandHandler

    private val logger = logger()

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun handle(command: ExecuteTestCaseCommand): ExecuteTestCaseCommand.Result {
        val testCaseToRun = self.beforeTestCaseRun(command.benchmarkRunId)
            ?: return ExecuteTestCaseCommand.Result(false)

        val result = runCatching {
            testCaseRunner.runTestCase(testCaseToRun)
        }

        self.afterTestCaseRun(testCaseToRun.id, result)

        if (result.isFailure) {
            throw result.exceptionOrNull() ?: UnknownError("Unknown error")
        }

        return ExecuteTestCaseCommand.Result(true)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun beforeTestCaseRun(benchmarkRunId: UUID): TestCase? {
        val testCaseToRun = testCaseReadService.findNextTestCaseToRun(benchmarkRunId) ?: return null

        // change state for testSuite if necessary
        val testSuite = testCaseToRun.testSuite
        if (testSuite.status == JobStatus.PENDING) {
            testSuite.started()
            testSuiteWriteService.save(testSuite)
        }

        // change state for benchmarkRun if necessary
        val benchmarkRun = testSuite.benchmarkRun
        if (benchmarkRun.status == JobStatus.PENDING) {
            benchmarkRun.started()
            benchmarkRunWriteService.save(benchmarkRun)
        }

        testCaseToRun.started()
        testCaseWriteService.save(testCaseToRun)

        return testCaseToRun
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun afterTestCaseRun(
        testCaseId: UUID,
        result: Result<TestCaseRunner.Result>,
    ) {
        val testCase = testCaseReadService.getById(testCaseId)
        val testSuite = testCase.testSuite
        val benchmarkRun = testSuite.benchmarkRun

        if (result.isSuccess) {
            val scalabilityThresholds = if (benchmarkRun.getBaselineTestCase().id == testCaseId) {
                null // If this is the baseline test case, there are no thresholds yet
            } else {
                benchmarkRun.getScalabilityThresholds()
            }

            val result = result.getOrThrow()
            testCase.completed(
                result.performanceMetrics,
                result.k6Output,
                result.jsonSummary,
                scalabilityThresholds,
            )

            // check if all test cases in test suite are completed
            if (testSuite.testCases.all { it.status == JobStatus.COMPLETED }) {
                testSuite.completed(benchmarkRun.getScalabilityThresholds())
            }

            // check if all test cases in benchmark run are completed
            if (benchmarkRun.testSuites.all { it.status == JobStatus.COMPLETED }) {
                benchmarkRun.completed()
            }
        } else {
            logger.error(result.exceptionOrNull()) { "Failed to execute TestCase $testCaseId" }
            testCase.failed()
            testSuite.failed()
            benchmarkRun.failed()
        }

        testCaseWriteService.save(testCase)
    }
}
