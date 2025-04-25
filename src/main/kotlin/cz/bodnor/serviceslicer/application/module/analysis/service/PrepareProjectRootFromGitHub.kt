package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSource
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PrepareProjectRootFromGitHub(
    @Value("\${app.projects.working-dir}") private val projectWorkingDir: String,
) {

    private val logger = logger()

    operator fun invoke(source: GitHubProjectSource): Path {
        val destinationDir = Path.of(projectWorkingDir, source.projectId.toString())

        logger.info("Cloning git repository...")
        val git = Git.cloneRepository()
            .setURI(source.repositoryGitUri)
            .setDirectory(destinationDir.toFile())
            .call()

        git.close()
        logger.info("Git repository cloned")

        return destinationDir
    }
}
