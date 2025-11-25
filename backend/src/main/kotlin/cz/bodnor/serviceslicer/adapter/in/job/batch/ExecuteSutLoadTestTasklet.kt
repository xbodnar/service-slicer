package cz.bodnor.serviceslicer.adapter.`in`.job.batch

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RunSutBenchmarkCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.domain.benchmarkrun.LoadResultStatus
import cz.bodnor.serviceslicer.domain.benchmarkrun.SutLoadTestRun
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel.BENCHMARK_ID
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel.BENCHMARK_RUN_ID
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

data class PendingLoadTest(
    val systemUnderTestId: UUID,
    val load: Int,
)

@Component
@JobScope
class ExecuteSutLoadTestTasklet(
    private val commandBus: CommandBus,
    @param:Value("#{jobParameters['${BENCHMARK_ID}']}") private val benchmarkId: UUID,
    @param:Value("#{jobParameters['${BENCHMARK_RUN_ID}']}") private val benchmarkRunId: UUID,
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
) : Tasklet {

    private val logger = logger()

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        val benchmark = benchmarkReadService.getById(benchmarkId)
        val benchmarkRun = benchmarkRunReadService.getById(benchmarkRunId)

        // Find first SUT and load pair that is pending (not yet started or completed)
        val pendingTest: PendingLoadTest? = benchmark.systemsUnderTest
            .flatMap { sut ->
                benchmark.config.operationalProfile.map { profile ->
                    PendingLoadTest(sut.id, profile.load)
                }
            }
            .firstOrNull { pending ->
                val sutRun = benchmarkRun.sutLoadTestRuns.find { it.systemUnderTestId == pending.systemUnderTestId }
                val loadResult = sutRun?.loadResults?.find { it.load == pending.load }
                // Consider it pending if no result exists or status is PENDING
                loadResult == null || loadResult.status == LoadResultStatus.PENDING
            }

        if (pendingTest == null) {
            benchmarkRun.markCompleted()
            benchmarkRunWriteService.save(benchmarkRun)
            return RepeatStatus.FINISHED
        }

        // Get or create SutLoadTestRun for this SUT
        val sutRun = benchmarkRun.sutLoadTestRuns
            .find { it.systemUnderTestId == pendingTest.systemUnderTestId }
            ?: SutLoadTestRun(systemUnderTestId = pendingTest.systemUnderTestId)
                .also { benchmarkRun.sutLoadTestRuns.add(it) }

        // Get or create LoadResult for this load level and mark as RUNNING
        val loadResult = sutRun.getOrCreateLoadResult(pendingTest.load)
        loadResult.markRunning(Instant.now())
        sutRun.updateOverallStatus()
        benchmarkRunWriteService.save(benchmarkRun)

        runCatching {
            val result = commandBus(
                RunSutBenchmarkCommand(
                    benchmarkId = benchmarkId,
                    systemUnderTestId = pendingTest.systemUnderTestId,
                    targetVus = pendingTest.load,
                ),
            )
            loadResult.markCompleted(result.endTimestamp, result.operationMeasurements, result.k6Output)
            sutRun.updateOverallStatus()
            benchmarkRunWriteService.save(benchmarkRun)
        }.onFailure { error ->
            logger.error(error) {
                "Failed to execute load test for SUT ${pendingTest.systemUnderTestId} with load ${pendingTest.load}"
            }
            loadResult.markFailed(Instant.now())
            sutRun.updateOverallStatus()
            benchmarkRun.markFailed()
            benchmarkRunWriteService.save(benchmarkRun)
            return RepeatStatus.FINISHED
        }

        return RepeatStatus.CONTINUABLE
    }
}
