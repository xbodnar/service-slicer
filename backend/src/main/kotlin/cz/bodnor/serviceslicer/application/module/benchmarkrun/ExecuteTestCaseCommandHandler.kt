package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ExecuteTestCaseCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.TestCaseRunner
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.domain.testcase.TestCase
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class PendingTest(
    val systemUnderTestId: UUID,
    val load: Int,
    val isBaseline: Boolean,
)

@Component
class ExecuteTestCaseCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
    private val testCaseRunner: TestCaseRunner,
) : CommandHandler<ExecuteTestCaseCommand.Result, ExecuteTestCaseCommand> {

    override val command = ExecuteTestCaseCommand::class

    @Lazy
    @Autowired
    private lateinit var self: ExecuteTestCaseCommandHandler

    private val logger = logger()

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun handle(command: ExecuteTestCaseCommand): ExecuteTestCaseCommand.Result {
        val (benchmarkRun, testCaseToRun) = self.beforeTestCaseRun(command.benchmarkRunId)
            ?: return ExecuteTestCaseCommand.Result(false)

        val result = runCatching {
            testCaseRunner.runTestCase(benchmarkRun, testCaseToRun)
        }

        self.afterTestCaseRun(benchmarkRun.id, testCaseToRun.id, result)

        return ExecuteTestCaseCommand.Result(true)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun beforeTestCaseRun(benchmarkRunId: UUID): Pair<BenchmarkRun, TestCase>? {
        val benchmarkRun = benchmarkRunReadService.getById(benchmarkRunId)
        val testCaseToRun = benchmarkRun.getNextTestCaseToRun()
            ?: return null

        testCaseToRun.started()
        benchmarkRunWriteService.save(benchmarkRun)

        return benchmarkRun to testCaseToRun
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun afterTestCaseRun(
        benchmarkRunId: UUID,
        testCaseId: UUID,
        result: Result<TestCaseRunner.Result>,
    ) {
        val benchmarkRun = benchmarkRunReadService.getById(benchmarkRunId)
        if (result.isSuccess) {
            val result = result.getOrThrow()
            benchmarkRun.markTestCaseCompleted(
                testCaseId,
                result.performanceMetrics,
                result.k6Output,
                result.jsonSummary,
            )
            benchmarkRunWriteService.save(benchmarkRun)
        } else {
            logger.error(result.exceptionOrNull()) { "Failed to execute TestCase $testCaseId" }
            benchmarkRun.markTestCaseFailed(testCaseId)
            benchmarkRunWriteService.save(benchmarkRun)
        }
    }
}
