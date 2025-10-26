package cz.bodnor.serviceslicer.domain.compose

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class ComposeFile(
    id: UUID = UUID.randomUUID(),
    val projectId: UUID,
    val fileId: UUID,
    val healthCheckUrl: String,
) : UpdatableEntity(id)

@Repository
interface ComposeFileRepository : JpaRepository<ComposeFile, UUID> {
    fun findByProjectId(projectId: UUID): List<ComposeFile>
}
