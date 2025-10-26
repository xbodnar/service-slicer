package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.project.PathHibernateConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import java.nio.file.Path
import java.util.UUID

@Entity
class GitProjectSource(
    id: UUID = UUID.randomUUID(),
    projectId: UUID,
    @Convert(PathHibernateConverter::class)
    val projectRootRelativePath: Path,
    val repositoryGitUri: String,
    val branchName: String,
) : ProjectSource(
    id = id,
    projectId = projectId,
    sourceType = SourceType.GIT,
) {

    // Path to the cloned repository
    @Convert(converter = PathHibernateConverter::class)
    var projectRootPath: Path? = null
        private set

    fun setProjectRoot(path: Path) {
        this.projectRootPath = path
    }

    override fun isInitialized(): Boolean = projectRootPath != null
}
