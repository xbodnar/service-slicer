package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.command.RestartDecompositionJobCommand
import cz.bodnor.serviceslicer.application.module.decomposition.event.DecompositionJobRestartedEvent
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RestartDecompositionJobCommandHandler(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<DecompositionJob, RestartDecompositionJobCommand> {

    override val command = RestartDecompositionJobCommand::class

    @Transactional
    override fun handle(command: RestartDecompositionJobCommand): DecompositionJob {
        val decompositionJob = decompositionJobReadService.getById(command.decompositionJobId)
        verify(decompositionJob.status == JobStatus.FAILED) {
            "Decomposition job is not failed, therefore cannot be restarted"
        }

        decompositionJob.queued()

        applicationEventPublisher.publishEvent(DecompositionJobRestartedEvent(decompositionJob.id))

        return decompositionJob
    }
}
