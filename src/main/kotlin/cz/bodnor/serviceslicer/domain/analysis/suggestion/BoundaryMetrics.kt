package cz.bodnor.serviceslicer.domain.analysis.suggestion

import jakarta.persistence.Embeddable

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
