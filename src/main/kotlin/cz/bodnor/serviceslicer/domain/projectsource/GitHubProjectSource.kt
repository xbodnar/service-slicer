package cz.bodnor.serviceslicer.domain.projectsource

import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class GitHubProjectSource(
    id: UUID = UUID.randomUUID(),
    projectId: UUID,
    javaProjectRootRelativePath: String?,

    val repositoryGitUri: String,

    val branchName: String,
) : ProjectSource(
    id = id,
    projectId = projectId,
    javaProjectRootRelativePath = javaProjectRootRelativePath,
    sourceType = SourceType.GITHUB_REPOSITORY,
)

@Repository
interface GitHubProjectSourceRepository : JpaRepository<GitHubProjectSource, UUID>
