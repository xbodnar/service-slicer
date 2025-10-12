package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.FindJavaProjectRootDirCommand
import cz.bodnor.serviceslicer.application.module.project.projectsource.ProjectSourceFinderService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class FindJavaProjectRootDirCommandHandler(
    private val projectFinderService: ProjectFinderService,
    private val projectSourceFinderService: ProjectSourceFinderService,
) : CommandHandler<Unit, FindJavaProjectRootDirCommand> {

    override val command = FindJavaProjectRootDirCommand::class

    private val logger = logger()

    override fun handle(command: FindJavaProjectRootDirCommand) {
        val project = projectFinderService.getById(command.projectId)
        require(project.projectRoot != null) { "Project root directory is not set." }

        if (project.javaProjectRoot != null) {
            logger.info("Java project root directory already set for project: ${project.id}")
            return
        }

        val projectSource = projectSourceFinderService.getByProjectId(project.id)

        // TODO: If relativePath starts with '/', resolving it removes the projectRoot part from the final Path
        val javaProjectRoot = projectSource.javaProjectRootRelativePath?.let { project.projectRoot!!.resolve(it) }
            ?: project.projectRoot!!

        project.setJavaProjectRoot(javaProjectRoot)
    }
}
