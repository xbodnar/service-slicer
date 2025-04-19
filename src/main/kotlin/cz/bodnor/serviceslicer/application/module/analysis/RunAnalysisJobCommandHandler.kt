package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.AnalysisJobFinderService
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.job.JobContainer
import cz.bodnor.serviceslicer.infrastructure.job.JobParameterLabel
import cz.bodnor.serviceslicer.infrastructure.job.JobType
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RunAnalysisJobCommandHandler(
    private val analysisJobFinderService: AnalysisJobFinderService,
    private val jobContainer: JobContainer,
    private val jobLauncher: JobLauncher,
) : CommandHandler<Unit, RunAnalysisJobCommand> {
    override val command = RunAnalysisJobCommand::class

    override fun handle(command: RunAnalysisJobCommand) {
        val analysisJob = analysisJobFinderService.getById(command.analysisJobId)

        val batchJob = when (analysisJob.analysisType) {
            AnalysisType.STATIC -> jobContainer[JobType.STATIC_CODE_ANALYSIS]
            AnalysisType.DYNAMIC -> jobContainer[JobType.DYNAMIC_CODE_ANALYSIS]
        }

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, analysisJob.projectId, UUID::class.java)
            .toJobParameters()

        jobLauncher.run(batchJob, jobParameters)
    }
}
