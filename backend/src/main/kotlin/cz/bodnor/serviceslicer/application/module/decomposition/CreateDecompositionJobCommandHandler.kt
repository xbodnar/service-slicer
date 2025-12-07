package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.command.CreateDecompositionJobCommand
import cz.bodnor.serviceslicer.application.module.decomposition.event.DecompositionJobCreatedEvent
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobWriteService
import cz.bodnor.serviceslicer.domain.decomposition.MonolithArtifact
import cz.bodnor.serviceslicer.domain.decomposition.MonolithArtifactWriteService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateDecompositionJobCommandHandler(
    private val monolithArtifactWriteService: MonolithArtifactWriteService,
    private val decompositionJobWriteService: DecompositionJobWriteService,
    private val fileReadService: FileReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) :
    CommandHandler<DecompositionJob, CreateDecompositionJobCommand> {
    override val command = CreateDecompositionJobCommand::class

    @Transactional
    override fun handle(command: CreateDecompositionJobCommand): DecompositionJob {
        val file = fileReadService.getById(command.jarFileId)
        verify(file.status == FileStatus.READY) { "File is not uploaded yet" }

        val monolithArtifact = monolithArtifactWriteService.create(
            MonolithArtifact(
                basePackageName = command.basePackageName,
                excludePackages = command.excludePackages,
                jarFile = file,
            ),
        )

        val decompositionJob = decompositionJobWriteService.save(
            DecompositionJob(
                name = command.name,
                monolithArtifact = monolithArtifact,
            ),
        )

        applicationEventPublisher.publishEvent(DecompositionJobCreatedEvent(decompositionJob.id))

        return decompositionJob
    }
}
