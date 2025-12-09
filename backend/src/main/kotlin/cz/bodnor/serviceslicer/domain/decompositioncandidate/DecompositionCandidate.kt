package cz.bodnor.serviceslicer.domain.decompositioncandidate

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.domain.decompositioncandidate.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.decompositioncandidate.ServiceBoundary
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Entity
class DecompositionCandidate(
    @ManyToOne
    val decompositionJob: DecompositionJob,

    @Enumerated(EnumType.STRING)
    val method: DecompositionMethod,

    val modularity: BigDecimal?,

) : UpdatableEntity() {

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "decompositionCandidate")
    val serviceBoundaries: MutableList<ServiceBoundary> = mutableListOf()

    fun addServiceBoundary(
        name: String,
        metrics: BoundaryMetrics,
        typeNames: List<String>,
    ) {
        this.serviceBoundaries.add(
            ServiceBoundary(
                name = name,
                metrics = metrics,
                typeNames = typeNames,
                decompositionCandidate = this,
            ),
        )
    }
}

@Repository
interface DecompositionCandidateRepository : JpaRepository<DecompositionCandidate, UUID> {

    fun findAllByDecompositionJobId(decompositionJobId: UUID): List<DecompositionCandidate>
}

enum class DecompositionMethod {
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
}
