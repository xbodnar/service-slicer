package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.file.FileService
import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectFromZipCommand
import cz.bodnor.serviceslicer.domain.project.ProjectCreateService
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateProjectFromZipCommandHandler(
    private val projectCreateService: ProjectCreateService,
    private val projectSourceCreateService: ProjectSourceCreateService,
    private val fileService: FileService,
) :
    CommandHandler<CreateProjectFromZipCommand.Result, CreateProjectFromZipCommand> {

    override val command = CreateProjectFromZipCommand::class

    @Transactional
    override fun handle(command: CreateProjectFromZipCommand): CreateProjectFromZipCommand.Result {
        val file = fileService.upload(command.file)

        val project = projectCreateService.create(
            name = command.projectName,
        )

        projectSourceCreateService.createFromZip(
            projectId = project.id,
            fileId = file.id,
        )

        return CreateProjectFromZipCommand.Result(
            projectId = project.id,
        )
    }
}
