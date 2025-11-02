package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectCommand
import cz.bodnor.serviceslicer.application.module.project.event.ProjectCreatedEvent
import cz.bodnor.serviceslicer.domain.project.ProjectWriteService
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class CreateProjectCommandHandler(
    private val projectWriteService: ProjectWriteService,
    private val projectSourceReadService: ProjectSourceReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<CreateProjectCommand.CreateProjectResult, CreateProjectCommand> {

    override val command = CreateProjectCommand::class

    @Transactional
    override fun handle(command: CreateProjectCommand): CreateProjectCommand.CreateProjectResult {
        val projectSource = projectSourceReadService.getById(command.projectSourceId)

        val project = projectWriteService.create(
            name = command.projectName,
            basePackageName = command.basePackageName,
            excludePackages = command.excludePackages,
            projectSourceId = projectSource.id,
        )

        applicationEventPublisher.publishEvent(ProjectCreatedEvent(project.id))

        return CreateProjectCommand.CreateProjectResult(
            projectId = project.id,
        )
    }
}
