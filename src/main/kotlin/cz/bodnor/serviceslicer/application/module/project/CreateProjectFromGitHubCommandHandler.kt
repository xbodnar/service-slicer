package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectFromGitHubCommand
import cz.bodnor.serviceslicer.domain.project.ProjectCreateService
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateProjectFromGitHubCommandHandler(
    private val projectCreateService: ProjectCreateService,
    private val projectSourceCreateService: ProjectSourceCreateService,
) : CommandHandler<CreateProjectFromGitHubCommand.Result, CreateProjectFromGitHubCommand> {
    override val command = CreateProjectFromGitHubCommand::class

    @Transactional
    override fun handle(command: CreateProjectFromGitHubCommand): CreateProjectFromGitHubCommand.Result {
        val project = projectCreateService.create(
            name = command.projectName,
        )

        projectSourceCreateService.createFromGitHub(
            projectId = project.id,
            githubRepositoryUrl = command.gitHubUrl,
            branchName = "main",
        )

        return CreateProjectFromGitHubCommand.Result(
            projectId = project.id,
        )
    }
}
