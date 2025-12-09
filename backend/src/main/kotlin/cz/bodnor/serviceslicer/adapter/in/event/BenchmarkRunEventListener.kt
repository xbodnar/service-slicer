package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.benchmark.event.BenchmarkRunCreatedEvent
import cz.bodnor.serviceslicer.application.module.benchmark.event.BenchmarkRunRestartedEvent
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RunSutValidationCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.event.ValidateSutBenchmarkEvent
import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.application.module.job.JobLauncherService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWriteService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Component
class BenchmarkRunEventListener(
    private val sutReadService: SystemUnderTestReadService,
    private val sutWriteService: SystemUnderTestWriteService,
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkWriteService: BenchmarkWriteService,
    private val jobContainer: JobContainer,
    private val jobLauncherService: JobLauncherService,
    private val commandBus: CommandBus,
) {

    private val logger = logger()

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onBenchmarkRunCreatedEvent(event: BenchmarkRunCreatedEvent) {
        val batchJob = jobContainer[JobType.BENCHMARK]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.BENCHMARK_RUN_ID, event.benchmarkRunId, UUID::class.java)
            .toJobParameters()

        logger.info { "Starting Job ${batchJob.name} for benchmarkRunId ${event.benchmarkRunId}" }

        jobLauncherService.launchAsync(batchJob, jobParameters)
    }

    @Async
    @EventListener
    fun onBenchmarkRunRestartedEvent(event: BenchmarkRunRestartedEvent) {
        val batchJob = jobContainer[JobType.BENCHMARK]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.BENCHMARK_RUN_ID, event.benchmarkRunId, UUID::class.java)
            .toJobParameters()

        logger.info { "Restarting Job ${batchJob.name} for benchmarkRunId ${event.benchmarkRunId}" }

        jobLauncherService.launchAsync(batchJob, jobParameters)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onValidateSutBenchmarkEvent(event: ValidateSutBenchmarkEvent) {
        commandBus(
            RunSutValidationCommand(
                benchmarkId = event.benchmarkId,
                systemUnderTestId = event.systemUnderTestId,
            ),
        )
    }
}
