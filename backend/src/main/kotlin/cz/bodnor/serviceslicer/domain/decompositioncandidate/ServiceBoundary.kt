package cz.bodnor.serviceslicer.domain.decompositioncandidate

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.decompositioncandidate.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidate
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
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
    /**
     * ID of the parent monolith decomposition
     */
    @ManyToOne
    val decompositionCandidate: DecompositionCandidate,

    /**
     * Suggested name for this microservice (derived from package or domain analysis), or an arbitrary name if no
     * suggestion is available
     */
    val name: String,

    /**
     * Metrics for this service boundary
     */
    @Embedded
    val metrics: BoundaryMetrics,

    @JdbcTypeCode(SqlTypes.JSON)
    val typeNames: List<String>,
) : UpdatableEntity()

@Repository
interface ServiceBoundaryRepository : JpaRepository<ServiceBoundary, UUID>

/**
 * Metrics for evaluating the quality of a service boundary
 */
@Embeddable
data class BoundaryMetrics(
    /**
     * Number of classes in this service
     */
    val size: Int,

    /**
     * Cohesion score: ratio of internal dependencies to total dependencies (0.0 to 1.0)
     * Higher is better - indicates classes in this service depend on each other
     */
    val cohesion: Double,

    /**
     * Coupling score: number of external dependencies this service has on other services
     * Lower is better - indicates less coupling to other services
     */
    val coupling: Int,

    /**
     * Number of dependencies between classes within this service
     */
    val internalDependencies: Int,

    /**
     * Number of dependencies from this service to other services
     */
    val externalDependencies: Int,
) {
    init {
        require(size > 0) { "Size must be positive" }
        require(cohesion in 0.0..1.0) { "Cohesion must be between 0.0 and 1.0" }
        require(coupling >= 0) { "Coupling must be non-negative" }
        require(internalDependencies >= 0) { "Internal dependencies must be non-negative" }
        require(externalDependencies >= 0) { "External dependencies must be non-negative" }
    }
}
