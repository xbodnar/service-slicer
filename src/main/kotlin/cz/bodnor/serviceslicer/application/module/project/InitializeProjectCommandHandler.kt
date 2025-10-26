package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.command.InitializeProjectCommand
import cz.bodnor.serviceslicer.application.module.project.service.FetchGitRepository
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.application.module.project.service.UnzipFile
import cz.bodnor.serviceslicer.application.module.projectsource.ProjectSourceFinderService
import cz.bodnor.serviceslicer.domain.projectsource.GitProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.JarProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import kotlin.io.path.Path

@Component
class InitializeProjectCommandHandler(
    private val projectSourceFinderService: ProjectSourceFinderService,
    private val projectFinderService: ProjectFinderService,
    private val unzipFile: UnzipFile,
    private val fetchGitRepository: FetchGitRepository,
) : CommandHandler<Unit, InitializeProjectCommand> {
    override val command = InitializeProjectCommand::class

    override fun handle(command: InitializeProjectCommand) {
        val project = projectFinderService.getById(command.projectId)
        val projectSource = projectSourceFinderService.getByProjectId(command.projectId)

        when (projectSource) {
            is ZipFileProjectSource -> projectSource.setProjectRoot(
                unzipFile(source = projectSource.zipFilePath, destination = Path(project.id.toString())),
            )

            is GitProjectSource -> projectSource.setProjectRoot(
                fetchGitRepository(uri = projectSource.repositoryGitUri, destination = Path(project.id.toString())),
            )

            is JarProjectSource -> {} // No need to initialize anything
        }
    }
}
