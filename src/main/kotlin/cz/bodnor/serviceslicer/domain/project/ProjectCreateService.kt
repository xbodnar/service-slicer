package cz.bodnor.serviceslicer.domain.project

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectCreateService(
    private val projectRepository: ProjectRepository,
) {

    @Transactional
    fun createFromZip(
        name: String,
        fileId: UUID,
    ) = projectRepository.save(
        Project(
            name = name,
            sourceType = SourceType.ZIP_FILE,
            githubRepositoryUrl = null,
            sourceFileId = fileId,
        ),
    )

    @Transactional
    fun createFromGitHub(
        name: String,
        githubRepositoryUrl: String,
    ) = projectRepository.save(
        Project(
            name = name,
            sourceType = SourceType.GITHUB_REPOSITORY,
            githubRepositoryUrl = githubRepositoryUrl,
            sourceFileId = null,
        ),
    )
}
