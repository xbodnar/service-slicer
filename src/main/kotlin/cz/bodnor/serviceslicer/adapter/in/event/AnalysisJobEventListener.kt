package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.event.AnalysisJobCreatedEvent
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AnalysisJobEventListener(
    private val commandBus: CommandBus,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAnalysisJobCreatedEvent(event: AnalysisJobCreatedEvent) {
        commandBus(RunAnalysisJobCommand(event.analysisJobId))
    }
}
