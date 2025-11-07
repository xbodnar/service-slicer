package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.RunJobCommand
import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.application.module.job.JobLauncherService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RunJobCommandHandler(
    private val jobContainer: JobContainer,
    private val jobLauncherService: JobLauncherService,
) : CommandHandler<RunJobCommand.Result, RunJobCommand> {

    private val logger = logger()

    override val command = RunJobCommand::class

    override fun handle(command: RunJobCommand): RunJobCommand.Result {
        val batchJob = jobContainer[command.jobType]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, command.projectId, UUID::class.java)
            .toJobParameters()

        logger.info(
            "Starting Job ${batchJob.name} for project ${command.projectId}",
        )

        jobLauncherService.launchAsync(batchJob, jobParameters)

        return RunJobCommand.Result(
            projectId = command.projectId,
        )
    }
}
