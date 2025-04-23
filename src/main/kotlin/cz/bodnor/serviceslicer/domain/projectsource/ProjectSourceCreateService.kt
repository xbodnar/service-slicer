package cz.bodnor.serviceslicer.domain.projectsource

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectSourceCreateService(
    private val gitHubSourceRepository: GitHubProjectSourceRepository,
    private val zipFileSourceRepository: ZipFileProjectSourceRepository,
) {

    @Transactional
    fun createFromGitHub(
        projectId: UUID,
        githubRepositoryUrl: String,
        branchName: String,
    ) = gitHubSourceRepository.save(
        GitHubProjectSource(
            projectId = projectId,
            gitHubRepositoryUrl = githubRepositoryUrl,
            branchName = branchName,
        ),
    )

    @Transactional
    fun createFromZip(
        projectId: UUID,
        fileId: UUID,
    ) = zipFileSourceRepository.save(
        ZipFileProjectSource(
            projectId = projectId,
            fileId = fileId,
        ),
    )
}
