package cz.bodnor.serviceslicer.domain.analysis.suggestion

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.OneToMany
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a microservice boundary suggestion generated from graph analysis
 */
@Entity
class MicroserviceSuggestion(
    id: UUID = UUID.randomUUID(),

    /**
     * The analysis job that generated this suggestion
     */
    val analysisJobId: UUID,

    /**
     * The algorithm used to generate this suggestion
     */
    @Enumerated(EnumType.STRING)
    val algorithm: BoundaryDetectionAlgorithm,

    /**
     * Overall modularity score for the entire decomposition (0.0 to 1.0)
     * Higher values indicate better separation between services
     */
    val modularityScore: Double,
) : UpdatableEntity(id) {

    /**
     * The suggested service boundaries
     */
    @OneToMany(mappedBy = "suggestion", cascade = [CascadeType.ALL], orphanRemoval = true)
    var boundaries: MutableList<ServiceBoundary> = mutableListOf()
        private set

    fun addBoundary(boundary: ServiceBoundary) {
        boundaries.add(boundary)
        boundary.suggestion = this
    }
}

@Repository
interface MicroserviceSuggestionRepository : JpaRepository<MicroserviceSuggestion, UUID> {
    fun findByAnalysisJobId(analysisJobId: UUID): List<MicroserviceSuggestion>
}

/**
 * Algorithm used to detect microservice boundaries
 */
enum class BoundaryDetectionAlgorithm {
    /**
     * Groups classes by package structure
     */
    PACKAGE_BASED,

    /**
     * Uses community detection algorithms (Louvain, Label Propagation)
     */
    COMMUNITY_DETECTION,

    /**
     * Applies DDD heuristics to identify bounded contexts
     */
    DOMAIN_DRIVEN_DESIGN,
}
