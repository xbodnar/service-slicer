package cz.bodnor.serviceslicer.domain.project

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.isDirectory

/**
 * Represents a Java project that will be analyzed.
 */
@Entity
class Project(
    id: UUID = UUID.randomUUID(),
    val name: String,

    /**
     * Source type (ZIP file or GitHub repository)
     */
    @Enumerated(EnumType.STRING)
    val sourceType: SourceType,

    /**
     * ID of the uploaded ZIP file
     */
    val sourceFileId: UUID?,

    /**
     * URL of the GitHub repository
     */
    val githubRepositoryUrl: String?,
) : UpdatableEntity(id) {

    /**
     * Current status of the project
     */
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus = ProjectStatus.CREATED
        private set

    var projectRootDir: String? = null
        private set

    fun setProjectRoot(path: Path) {
        require(path.isDirectory()) { "Working directory must be a directory" }
        this.projectRootDir = path.toString()
    }
}

@Repository
interface ProjectRepository : JpaRepository<Project, UUID>

/**
 * Source type of the project
 */
enum class SourceType {
    ZIP_FILE,
    GITHUB_REPOSITORY,
}

/**
 * Status of the project
 */
enum class ProjectStatus {
    CREATED,
    UPLOADING,
    UPLOADED,
    EXTRACTING,
    EXTRACTED,
    ANALYZING,
    ANALYZED,
    FAILED,
}
