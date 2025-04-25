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
        // Search for project roots
//        val javaSrcDirectories = mutableSetOf<Path>()
//        Files.walk(project.projectRoot!!).use { stream ->
//            stream.forEach { path ->
//                if (path.name in PROJECT_BUILD_FILES) {
//                    javaSrcDirectories.add(path.parent)
//                }
//            }
//        }
//
//        when {
//            javaSrcDirectories.isEmpty() -> error("No Java projects detected.")
//
//            javaSrcDirectories.size > 1 -> error(
//                "Multiple Java projects detected:  ${javaSrcDirectories.joinToString { it.name }}",
//            )
//
//            else -> project.setJavaProjectRoot(javaSrcDirectories.first())
//        }
    }
//
//    companion object {
//        private val PROJECT_BUILD_FILES = listOf("pom.xml", "build.gradle", "build.gradle.kts")
//    }
}
