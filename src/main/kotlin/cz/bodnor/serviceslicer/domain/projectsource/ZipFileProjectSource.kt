package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.project.PathHibernateConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import java.nio.file.Path
import java.util.UUID

@Entity
class ZipFileProjectSource(
    id: UUID = UUID.randomUUID(),
    projectId: UUID,
    @Convert(PathHibernateConverter::class)
    val projectRootRelativePath: Path,
    @Convert(PathHibernateConverter::class)
    val zipFilePath: Path,
) : ProjectSource(
    id = id,
    projectId = projectId,
    sourceType = SourceType.ZIP,
) {

    // Path to the unzipped directory
    @Convert(converter = PathHibernateConverter::class)
    var projectRootPath: Path? = null
        private set

    fun setProjectRoot(path: Path) {
        projectRootPath = path
    }

    override fun isInitialized(): Boolean = projectRootPath != null
}
