package cz.bodnor.serviceslicer.application.module.project.service

import cz.bodnor.serviceslicer.domain.projectsource.GitProjectSource
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class FetchGitRepository(
    @Value("\${app.projects.working-dir}") private val projectWorkingDir: String,
) {

    private val logger = logger()

    operator fun invoke(
        uri: String,
        destination: Path,
    ): Path {
        val destinationDir = Path.of(projectWorkingDir).resolve(destination)

        logger.info("Cloning git repository...")
        val git = Git.cloneRepository()
            .setURI(uri)
            .setDirectory(destinationDir.toFile())
            .call()

        git.close()
        logger.info("Git repository cloned")

        return destinationDir
    }
}
