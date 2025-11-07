package cz.bodnor.serviceslicer.domain.analysis.decomposition

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import cz.bodnor.serviceslicer.domain.common.DomainEntity
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a suggested microservice boundary containing a cohesive group of classes
 */
@Entity
class ServiceBoundary(
    id: UUID = UUID.randomUUID(),

    /**
     * ID of the parent monolith decomposition
     */
    val monolithDecompositionId: UUID,

    /**
     * Suggested name for this microservice (derived from package or domain analysis)
     */
    val suggestedName: String,

    /**
     * Metrics for this service boundary
     */
    @Embedded
    val metrics: BoundaryMetrics,

    @JdbcTypeCode(SqlTypes.JSON)
    val typeNames: List<String>,
) : UpdatableEntity(id)

@Repository
interface ServiceBoundaryRepository : JpaRepository<ServiceBoundary, UUID>
