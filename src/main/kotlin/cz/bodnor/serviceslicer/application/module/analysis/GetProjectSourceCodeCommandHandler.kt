package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.GetProjectSourceCodeCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.FetchProjectRepoFromGithub
import cz.bodnor.serviceslicer.application.module.analysis.service.UnzipProjectZipFile
import cz.bodnor.serviceslicer.application.module.project.projectsource.ProjectSourceFinderService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class GetProjectSourceCodeCommandHandler(
    private val projectSourceFinderService: ProjectSourceFinderService,
    private val projectFinderService: ProjectFinderService,
    private val unzipProjectZipFile: UnzipProjectZipFile,
    private val fetchProjectRepoFromGithub: FetchProjectRepoFromGithub,
) : CommandHandler<Unit, GetProjectSourceCodeCommand> {
    override val command = GetProjectSourceCodeCommand::class

    override fun handle(command: GetProjectSourceCodeCommand) {
        val project = projectFinderService.getById(command.projectId)
        val projectSource = projectSourceFinderService.getByProjectId(command.projectId)

        val projectRoot = when (projectSource) {
            is ZipFileProjectSource -> unzipProjectZipFile(projectSource)
            is GitHubProjectSource -> fetchProjectRepoFromGithub(projectSource)
            else -> error("Unsupported source type: ${projectSource.javaClass}")
        }

        project.setProjectRoot(projectRoot)
    }
}
