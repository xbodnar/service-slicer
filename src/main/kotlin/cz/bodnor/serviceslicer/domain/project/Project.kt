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

    /**
     * Path to the project root directory. May contain multiple subdirectories unrelated to the project being processed
     */
    @Convert(converter = PathHibernateConverter::class)
    var projectRoot: Path? = null
        private set

    /**
     * Path to the specific Java project root directory that is being processed (root project may contain multiple
     * java projects, for example when provided through GitHub repository link)
     */
    @Convert(converter = PathHibernateConverter::class)
    var javaProjectRoot: Path? = null
        private set

    fun setProjectRoot(path: Path) {
        require(path.isDirectory()) { "Working directory must be a directory" }
        this.projectRoot = path
    }

    fun setJavaProjectRoot(path: Path) {
        require(path.isDirectory()) { "Src directory must be a directory" }
        this.javaProjectRoot = path
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
