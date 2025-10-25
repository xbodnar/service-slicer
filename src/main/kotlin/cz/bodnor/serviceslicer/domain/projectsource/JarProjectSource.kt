package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.project.PathHibernateConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.nio.file.Path
import java.util.UUID

@Entity
class JarProjectSource(
    id: UUID = UUID.randomUUID(),
    projectId: UUID,
    @Convert(converter = PathHibernateConverter::class)
    val jarFilePath: Path,
) : ProjectSource(
    id = id,
    projectId = projectId,
    sourceType = SourceType.JAR,
) {
    override fun isInitialized(): Boolean = true
}
