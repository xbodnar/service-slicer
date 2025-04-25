package cz.bodnor.serviceslicer.domain.projectsource

import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class ZipFileProjectSource(
    id: UUID = UUID.randomUUID(),
    projectId: UUID,
    javaProjectRootRelativePath: String?,

    val fileId: UUID,
) : ProjectSource(
    id = id,
    projectId = projectId,
    javaProjectRootRelativePath = javaProjectRootRelativePath,
    sourceType = SourceType.ZIP_FILE,
)

@Repository
interface ZipFileProjectSourceRepository : JpaRepository<ZipFileProjectSource, UUID>
