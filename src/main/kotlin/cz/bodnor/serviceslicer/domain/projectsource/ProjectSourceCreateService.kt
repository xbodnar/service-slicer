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
        javaProjectRootRelativePath: String?,
        repositoryGitUri: String,
        branchName: String,
    ) = gitHubSourceRepository.save(
        GitHubProjectSource(
            projectId = projectId,
            repositoryGitUri = repositoryGitUri,
            branchName = branchName,
            javaProjectRootRelativePath = javaProjectRootRelativePath,
        ),
    )

    @Transactional
    fun createFromZip(
        projectId: UUID,
        javaProjectRootRelativePath: String?,
        fileId: UUID,
    ) = zipFileSourceRepository.save(
        ZipFileProjectSource(
            projectId = projectId,
            fileId = fileId,
            javaProjectRootRelativePath = javaProjectRootRelativePath,
        ),
    )
}
