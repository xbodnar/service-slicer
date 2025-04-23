package cz.bodnor.serviceslicer.domain.project

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Convert
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
) : UpdatableEntity(id) {

    /**
     * Current status of the project
     */
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus = ProjectStatus.CREATED
        private set

    @Convert(converter = PathHibernateConverter::class)
    var projectRootDir: Path? = null
        private set

    fun setProjectRoot(path: Path) {
        require(path.isDirectory()) { "Working directory must be a directory" }
        this.projectRootDir = path
    }
}

@Repository
interface ProjectRepository : JpaRepository<Project, UUID>

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
