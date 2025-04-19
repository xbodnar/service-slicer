package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.file.FileService
import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectFromZipCommand
import cz.bodnor.serviceslicer.domain.project.ProjectCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class CreateProjectFromZipCommandHandler(
    private val projectCreateService: ProjectCreateService,
    private val fileService: FileService,
) :
    CommandHandler<CreateProjectFromZipCommand.Result, CreateProjectFromZipCommand> {

    override val command = CreateProjectFromZipCommand::class

    override fun handle(command: CreateProjectFromZipCommand): CreateProjectFromZipCommand.Result {
        val file = fileService.upload(command.file)

        val project = projectCreateService.createFromZip(
            name = command.file.name,
            fileId = file.id,
        )

        return CreateProjectFromZipCommand.Result(
            projectId = project.id,
        )
    }
}
