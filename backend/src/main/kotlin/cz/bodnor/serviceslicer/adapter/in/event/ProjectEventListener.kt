package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.application.module.job.JobLauncherService
import cz.bodnor.serviceslicer.application.module.project.event.ProjectCreatedEvent
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.util.UUID

@Component
class ProjectEventListener(
    private val jobContainer: JobContainer,
    private val jobLauncherService: JobLauncherService,
) {

    private val logger = logger()

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onProjectCreatedEvent(event: ProjectCreatedEvent) {
        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, event.projectId, UUID::class.java)
            .toJobParameters()

        logger.info { "Starting Job ${batchJob.name} for project ${event.projectId}" }

        jobLauncherService.launchAsync(batchJob, jobParameters)
    }
}
