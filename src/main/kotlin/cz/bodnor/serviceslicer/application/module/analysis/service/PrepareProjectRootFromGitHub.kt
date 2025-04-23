package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSource
import org.eclipse.jgit.api.Git
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PrepareProjectRootFromGitHub(
    @Value("\${app.projects.working-dir}") private val projectWorkingDir: String,
) {

    operator fun invoke(source: GitHubProjectSource): Path {
        val destinationDir = Path.of(projectWorkingDir, source.projectId.toString())

        val git = Git.cloneRepository()
            .setURI(source.gitHubRepositoryUrl)
            .setDirectory(destinationDir.toFile())
            .call()

        git.close()

        return destinationDir
    }
}
