package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.RestartFailedAnalysisCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.AnalysisJobFinderService
import cz.bodnor.serviceslicer.application.module.analysis.service.JobLauncherService
import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RestartFailedAnalysisCommandHandler(
    private val analysisJobFinderService: AnalysisJobFinderService,
    private val jobContainer: JobContainer,
    private val jobExplorer: JobExplorer,
    private val jobLauncherService: JobLauncherService,
) : CommandHandler<RestartFailedAnalysisCommand.Result, RestartFailedAnalysisCommand> {

    private val logger = logger()

    override val command = RestartFailedAnalysisCommand::class

    override fun handle(command: RestartFailedAnalysisCommand): RestartFailedAnalysisCommand.Result {
        val analysisJob = analysisJobFinderService.getByProjectId(command.projectId)

        val job = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        // Restart the job using the job repository
        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, analysisJob.projectId, UUID::class.java)
            .toJobParameters()

        validateLastExecution(job, jobParameters)

        jobLauncherService.launch(job, jobParameters)

        return RestartFailedAnalysisCommand.Result(
            projectId = command.projectId,
        )
    }

    private fun validateLastExecution(
        job: Job,
        jobParameters: JobParameters,
    ) {
        // Find the last job execution for this analysis job
        val jobInstance = jobExplorer.getJobInstance(job.name, jobParameters)
            ?: error("No job instance found for ${job.name}")

        val lastExecution = jobExplorer.getLastJobExecution(jobInstance)
            ?: error("No job execution found for job instance ${jobInstance.instanceId}")

        require(lastExecution.status == BatchStatus.FAILED) {
            "Last job execution status is ${lastExecution.status}, expected FAILED"
        }
    }
}
