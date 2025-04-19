package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.CreateAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.event.AnalysisJobCreatedEvent
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJobCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateAnalysisJobCommandHandler(
    private val analysisJobCreateService: AnalysisJobCreateService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<CreateAnalysisJobCommand.Result, CreateAnalysisJobCommand> {
    override val command = CreateAnalysisJobCommand::class

    @Transactional
    override fun handle(command: CreateAnalysisJobCommand): CreateAnalysisJobCommand.Result {
        val analysisJob = analysisJobCreateService.create(
            projectId = command.projectId,
            analysisType = command.analysisType,
        )

        applicationEventPublisher.publishEvent(
            AnalysisJobCreatedEvent(
                analysisJobId = analysisJob.id,
            ),
        )

        return CreateAnalysisJobCommand.Result(
            analysisJobId = analysisJob.id,
        )
    }
}
