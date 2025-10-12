package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.event.AnalysisJobCreatedEvent
import cz.bodnor.serviceslicer.application.module.analysis.service.AnalysisJobFinderService
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJobCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.job.JobContainer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RunAnalysisJobCommandHandler(
    private val analysisJobCreateService: AnalysisJobCreateService,
    private val analysisJobFinderService: AnalysisJobFinderService,
    private val jobContainer: JobContainer,
    private val jobLauncher: JobLauncher,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<RunAnalysisJobCommand.Result, RunAnalysisJobCommand> {
    override val command = RunAnalysisJobCommand::class

    @Transactional
    override fun handle(command: RunAnalysisJobCommand): RunAnalysisJobCommand.Result {
        val analysisJob = analysisJobCreateService.create(
            projectId = command.projectId,
        )

        applicationEventPublisher.publishEvent(
            AnalysisJobCreatedEvent(
                analysisJobId = analysisJob.id,
            ),
        )

        return RunAnalysisJobCommand.Result(
            analysisJobId = analysisJob.id,
        )
    }
}
