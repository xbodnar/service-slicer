package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
sealed class ProjectSource(
    id: UUID,

    val projectId: UUID,

    @Enumerated(EnumType.STRING)
    val sourceType: SourceType,
) : CreatableEntity(id) {

    abstract fun isInitialized(): Boolean
}

@Repository
interface ProjectSourceRepository : JpaRepository<ProjectSource, UUID> {
    fun findByProjectId(projectId: UUID): ProjectSource?
}

/**
 * Source type of the project
 */
enum class SourceType {
    ZIP,
    JAR,
    GIT,
}
