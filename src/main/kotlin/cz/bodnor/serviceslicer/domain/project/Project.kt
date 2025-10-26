package cz.bodnor.serviceslicer.domain.project

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a Java project that will be analyzed.
 */
@Entity
class Project(
    id: UUID = UUID.randomUUID(),
    // Custom name to identify the project
    val name: String,
    // Base package name used to identify which classes should be included in the analysis
    val basePackageName: String,
    // Used to exclude some packages from the final dependency graph, for example generated classes
    @JdbcTypeCode(SqlTypes.JSON)
    val excludePackages: List<String> = emptyList(),
) : UpdatableEntity(id) {

    /**
     * Current status of the project
     */
    @Enumerated(EnumType.STRING)
    var status: ProjectStatus = ProjectStatus.CREATED
        private set
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
