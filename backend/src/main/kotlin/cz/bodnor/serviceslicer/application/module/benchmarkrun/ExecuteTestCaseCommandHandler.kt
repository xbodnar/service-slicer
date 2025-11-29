package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ExecuteTestCaseCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.TestCaseRunner
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import java.time.Instant
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

    private val logger = logger()

    override fun handle(command: ExecuteTestCaseCommand): ExecuteTestCaseCommand.Result {
        val benchmarkRun = benchmarkRunReadService.getById(command.benchmarkRunId)
        val benchmark = benchmarkReadService.getById(benchmarkRun.benchmarkId)

        // Find first test that needs to be run
        val nextTestCaseToRun = benchmarkRun.getNextTestCaseToRun(benchmark)
        require(nextTestCaseToRun != null) {
            "Failed to find next test case to run"
        }

        logger.info {
            "Executing TestCase with sutId=${nextTestCaseToRun.sutId}, load=${nextTestCaseToRun.load} and isBaseline=${nextTestCaseToRun.isBaseline}"
        }
        executeTest(benchmarkRun, nextTestCaseToRun)

        if (benchmarkRun.getNextTestCaseToRun(benchmark) == null) {
            // All tests completed
            benchmarkRun.markCompleted()
            benchmarkRunWriteService.save(benchmarkRun)
            return ExecuteTestCaseCommand.Result(hasMoreTests = false)
        }
        return ExecuteTestCaseCommand.Result(true)
    }

    private fun executeTest(
        benchmarkRun: BenchmarkRun,
        testCaseToRun: BenchmarkRun.TestCaseToRun,
    ) {
        benchmarkRun.addTestCase(
            isBaseline = testCaseToRun.isBaseline,
            sutId = testCaseToRun.sutId,
            load = testCaseToRun.load,
        )
        benchmarkRunWriteService.save(benchmarkRun)

        runCatching {
            val result = testCaseRunner.runTestCase(
                TestCaseRunner.Input(
                    benchmarkId = benchmarkRun.benchmarkId,
                    benchmarkRunId = benchmarkRun.id,
                    sutId = testCaseToRun.sutId,
                    load = testCaseToRun.load.load,
                ),
            )

            benchmarkRun.markTestCaseCompleted(
                sutId = testCaseToRun.sutId,
                load = testCaseToRun.load.load,
                endTimestamp = result.endTimestamp,
                measurements = result.operationMeasurements,
                k6Output = result.k6Output,
            )
            benchmarkRunWriteService.save(benchmarkRun)
        }.onFailure {
            logger.error(it) {
                "Failed to execute test case for SUT $testCaseToRun"
            }
            benchmarkRun.markTestCaseFailed(
                sutId = testCaseToRun.sutId,
                load = testCaseToRun.load.load,
                endTimestamp = Instant.now(),
            )
            benchmarkRunWriteService.save(benchmarkRun)
            throw it
        }
    }
}
