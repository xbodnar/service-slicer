package cz.bodnor.serviceslicer.domain.decomposition

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import cz.bodnor.serviceslicer.domain.file.File
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class MonolithArtifact(
    val basePackageName: String,

    @JdbcTypeCode(SqlTypes.JSON)
    val excludePackages: List<String>,

    @ManyToOne
    val jarFile: File,
) : CreatableEntity()

@Repository
interface MonolithArtifactRepository : JpaRepository<MonolithArtifact, UUID>
