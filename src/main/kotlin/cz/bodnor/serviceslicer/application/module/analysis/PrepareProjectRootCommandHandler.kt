package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.PrepareProjectRootCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.PrepareProjectRootFromGitHub
import cz.bodnor.serviceslicer.application.module.analysis.service.PrepareProjectRootFromZip
import cz.bodnor.serviceslicer.application.module.project.projectsource.ProjectSourceFinderService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class PrepareProjectRootCommandHandler(
    private val projectSourceFinderService: ProjectSourceFinderService,
    private val projectFinderService: ProjectFinderService,
    private val prepareProjectRootFromZip: PrepareProjectRootFromZip,
    private val prepareProjectRootFromGitHub: PrepareProjectRootFromGitHub,
) : CommandHandler<Unit, PrepareProjectRootCommand> {
    override val command = PrepareProjectRootCommand::class

    override fun handle(command: PrepareProjectRootCommand) {
        val project = projectFinderService.getById(command.projectId)
        val projectSource = projectSourceFinderService.getById(command.projectId)

        val projectRoot = when (projectSource) {
            is ZipFileProjectSource -> prepareProjectRootFromZip(projectSource)
            is GitHubProjectSource -> prepareProjectRootFromGitHub(projectSource)
            else -> error("Unsupported source type: ${projectSource.javaClass}")
        }

        project.setProjectRoot(projectRoot)
    }
}
