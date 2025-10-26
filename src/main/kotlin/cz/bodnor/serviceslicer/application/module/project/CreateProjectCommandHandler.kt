package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectCommand
import cz.bodnor.serviceslicer.application.module.project.event.ProjectCreatedEvent
import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import cz.bodnor.serviceslicer.domain.projectsource.GitProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.JarProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceRepository
import cz.bodnor.serviceslicer.domain.projectsource.SourceType
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CreateProjectCommandHandler(
    private val projectRepository: ProjectRepository,
    private val projectSourceRepository: ProjectSourceRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<CreateProjectCommand.CreateProjectResult, CreateProjectCommand> {

    override val command = CreateProjectCommand::class

    override fun handle(command: CreateProjectCommand): CreateProjectCommand.CreateProjectResult {
        val project = projectRepository.save(
            Project(
                name = command.projectName,
                basePackageName = command.basePackageName,
            ),
        )

        val projectSource = when (command.sourceType) {
            SourceType.ZIP -> {
                ZipFileProjectSource(
                    projectId = project.id,
                    projectRootRelativePath = command.projectRootRelativePath,
                    zipFilePath = command.file!!,
                )
            }

            SourceType.GIT -> {
                GitProjectSource(
                    projectId = project.id,
                    projectRootRelativePath = command.projectRootRelativePath,
                    repositoryGitUri = command.gitUri!!,
                    branchName = command.branchName!!,
                )
            }

            SourceType.JAR -> {
                JarProjectSource(
                    projectId = project.id,
                    jarFilePath = command.file!!,
                )
            }
        }

        projectSourceRepository.save(projectSource)

        applicationEventPublisher.publishEvent(ProjectCreatedEvent(project.id))

        return CreateProjectCommand.CreateProjectResult(
            projectId = UUID.randomUUID(),
        )
    }
}
