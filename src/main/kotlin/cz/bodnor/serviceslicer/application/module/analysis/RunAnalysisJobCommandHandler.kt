package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.JobLauncherService
import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJobCreateService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RunAnalysisJobCommandHandler(
    private val analysisJobCreateService: AnalysisJobCreateService,
    private val jobContainer: JobContainer,
    private val jobLauncherService: JobLauncherService,
) : CommandHandler<RunAnalysisJobCommand.Result, RunAnalysisJobCommand> {

    private val logger = logger()

    override val command = RunAnalysisJobCommand::class

    override fun handle(command: RunAnalysisJobCommand): RunAnalysisJobCommand.Result {
        val analysisJob = analysisJobCreateService.create(
            projectId = command.projectId,
        )

        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, analysisJob.projectId, UUID::class.java)
            .toJobParameters()

        logger.info(
            "Starting analysisJob with id=[${analysisJob.id}] for job ${batchJob.name} and project ${command.projectId}",
        )

        jobLauncherService.launch(batchJob, jobParameters)

        return RunAnalysisJobCommand.Result(
            projectId = command.projectId,
        )
    }
}
