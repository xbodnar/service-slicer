package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.analysis.command.RunJobCommand
import cz.bodnor.serviceslicer.application.module.project.event.ProjectCreatedEvent
import cz.bodnor.serviceslicer.domain.job.JobType
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
        commandBus(
            RunJobCommand(
                projectId = event.projectId,
                jobType = JobType.STATIC_CODE_ANALYSIS, // Start with static analysis
            ),
        )
    }
}
