package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class ProjectSource(
    id: UUID = UUID.randomUUID(),

    // ID of the file entity containing the JAR file
    val jarFileId: UUID,

) : CreatableEntity(id) {

    var projectDirId: UUID? = null
        private set

    fun setProjectDirId(projectDirId: UUID) {
        this.projectDirId = projectDirId
    }
}

@Repository
interface ProjectSourceRepository : JpaRepository<ProjectSource, UUID>
