package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.analysis.event.AnalysisJobCreatedEvent
import cz.bodnor.serviceslicer.application.module.analysis.service.RunAnalysisJobService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AnalysisJobEventListener(
    private val runAnalysisJobService: RunAnalysisJobService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAnalysisJobCreatedEvent(event: AnalysisJobCreatedEvent) {
        runAnalysisJobService.run(event.analysisJobId)
    }
}
