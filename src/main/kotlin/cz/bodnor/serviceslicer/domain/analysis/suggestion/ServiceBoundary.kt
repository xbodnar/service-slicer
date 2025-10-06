package cz.bodnor.serviceslicer.domain.analysis.suggestion

import cz.bodnor.serviceslicer.domain.common.DomainEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.util.UUID

/**
 * Represents a suggested microservice boundary containing a cohesive group of classes
 */
@Entity
class ServiceBoundary(
    id: UUID = UUID.randomUUID(),

    /**
     * Suggested name for this microservice (derived from package or domain analysis)
     */
    val suggestedName: String,

    /**
     * Metrics for this service boundary
     */
    @Embedded
    val metrics: BoundaryMetrics,
) : DomainEntity(id) {

    /**
     * The parent suggestion this boundary belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggestion_id", nullable = false)
    lateinit var suggestion: MicroserviceSuggestion

    /**
     * Fully qualified class names of types that belong to this service
     */
    @ElementCollection
    @CollectionTable(name = "service_boundary_types", joinColumns = [JoinColumn(name = "boundary_id")])
    @Column(name = "type_fqn")
    var typeNames: MutableSet<String> = mutableSetOf()
        private set

    fun addType(fullyQualifiedClassName: String) {
        typeNames.add(fullyQualifiedClassName)
    }
}
