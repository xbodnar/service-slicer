package cz.bodnor.serviceslicer.domain.analysis.decomposition

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
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
class MonolithDecomposition(
    id: UUID = UUID.randomUUID(),

    /**
     * ID of the project this decomposition belongs to
     */
    val projectId: UUID,

    /**
     * The algorithm used to generate this suggestion
     */
    @Enumerated(EnumType.STRING)
    val algorithm: DecompositionApproach,

    /**
     * Overall modularity score for the entire decomposition (0.0 to 1.0)
     * Higher values indicate better separation between services
     */
    val modularityScore: Double,
) : UpdatableEntity(id) {

    @OneToMany(mappedBy = "monolithDecompositionId")
    var serviceBoundaries: MutableList<ServiceBoundary> = mutableListOf()
        private set
}

@Repository
interface MonolithDecompositionRepository : JpaRepository<MonolithDecomposition, UUID> {
    fun findByProjectId(projectId: UUID): List<MonolithDecomposition>
}

/**
 * Algorithm used to detect microservice boundaries
 */
enum class DecompositionApproach {
    /**
     * Label Propagation - Fast but non-deterministic community detection
     */
    COMMUNITY_DETECTION_LABEL_PROPAGATION,

    /**
     * Louvain - Hierarchical modularity optimization, better balanced communities
     */
    COMMUNITY_DETECTION_LOUVAIN,

    /**
     * Leiden - Similar to Louvain, but with additional optimizations
     */
    COMMUNITY_DETECTION_LEIDEN,

    /**
     * Applies DDD heuristics to identify bounded contexts
     */
    DOMAIN_DRIVEN_DECOMPOSITION,

    /**
     * Applies actor-centered decomposition to identify user journeys
     */
    ACTOR_DRIVEN_DECOMPOSITION,

    /**
     * Custom/manual decomposition approach
     */
    CUSTOM,
}
