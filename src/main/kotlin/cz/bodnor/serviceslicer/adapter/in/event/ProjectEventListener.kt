package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.project.event.ProjectCreatedEvent
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProjectEventListener(
    private val commandBus: CommandBus,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onProjectCreatedEvent(event: ProjectCreatedEvent) {
        commandBus(RunAnalysisJobCommand(event.projectId))
    }
}
