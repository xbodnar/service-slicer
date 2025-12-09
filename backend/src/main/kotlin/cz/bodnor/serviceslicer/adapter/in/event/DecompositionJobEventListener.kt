package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.decomposition.event.DecompositionJobCreatedEvent
import cz.bodnor.serviceslicer.application.module.decomposition.event.DecompositionJobRestartedEvent
import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.application.module.job.JobLauncherService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Component
class DecompositionJobEventListener(
    private val jobContainer: JobContainer,
    private val jobLauncherService: JobLauncherService,
) {

    private val logger = logger()

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDecompositionJobCreatedEvent(event: DecompositionJobCreatedEvent) {
        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.DECOMPOSITION_JOB_ID, event.decompositionJobId, UUID::class.java)
            .toJobParameters()

        logger.info { "Starting Job ${batchJob.name} for decompositionJobId ${event.decompositionJobId}" }

        jobLauncherService.launchAsync(batchJob, jobParameters)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDecompositionJobRestartedEvent(event: DecompositionJobRestartedEvent) {
        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.DECOMPOSITION_JOB_ID, event.decompositionJobId, UUID::class.java)
            .toJobParameters()

        logger.info { "Restarting Job ${batchJob.name} for decompositionJobId ${event.decompositionJobId}" }

        jobLauncherService.launchAsync(batchJob, jobParameters)
    }
}
