package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.analysis.service.RunAnalysisJobService
import cz.bodnor.serviceslicer.application.module.project.event.ProjectCreatedEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ProjectEventListener(
    private val runAnalysisJobService: RunAnalysisJobService,
) {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onProjectCreatedEvent(event: ProjectCreatedEvent) {
        runAnalysisJobService.run(event.projectId)
    }
}
