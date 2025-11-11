package cz.bodnor.serviceslicer.application.module.compose

import cz.bodnor.serviceslicer.application.module.compose.command.UploadComposeFileCommand
import cz.bodnor.serviceslicer.application.module.project.service.ProjectReadService
import cz.bodnor.serviceslicer.domain.compose.ComposeFileCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UploadComposeFileCommandHandler(
    private val composeFileCreateService: ComposeFileCreateService,
    private val projectReadService: ProjectReadService,
) : CommandHandler<UploadComposeFileCommand.Result, UploadComposeFileCommand> {

    override val command = UploadComposeFileCommand::class

    @Transactional
    override fun handle(command: UploadComposeFileCommand): UploadComposeFileCommand.Result {
        val file = TODO()
        val project = projectReadService.getById(command.projectId)

        val composeFile = composeFileCreateService.create(
            projectId = project.id,
            fileId = TODO(),
            healthCheckUrl = command.healthCheckUrl,
        )

        return UploadComposeFileCommand.Result(
            composeFileId = composeFile.id,
        )
    }
}
