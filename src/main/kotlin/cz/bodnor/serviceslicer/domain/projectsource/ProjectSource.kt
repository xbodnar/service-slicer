package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import org.hibernate.annotations.Proxy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Proxy(lazy = false) // We don't need proxies since we do not use Hibernate for mapping references
abstract class ProjectSource(
    id: UUID,

    val projectId: UUID,

    @Enumerated(EnumType.STRING)
    val sourceType: SourceType,
) : CreatableEntity(id)

@Repository
interface ProjectSourceRepository : JpaRepository<ProjectSource, UUID> {
    fun findByProjectId(projectId: UUID): ProjectSource?
}

/**
 * Source type of the project
 */
enum class SourceType {
    ZIP_FILE,
    GITHUB_REPOSITORY,
}
