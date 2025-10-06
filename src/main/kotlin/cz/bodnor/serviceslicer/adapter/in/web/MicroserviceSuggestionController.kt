package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryDetectionAlgorithm
import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.analysis.suggestion.MicroserviceSuggestion
import cz.bodnor.serviceslicer.domain.analysis.suggestion.MicroserviceSuggestionRepository
import cz.bodnor.serviceslicer.domain.analysis.suggestion.ServiceBoundary
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/suggestions")
class MicroserviceSuggestionController(
    private val suggestionRepository: MicroserviceSuggestionRepository,
) {

    /**
     * Get all microservice boundary suggestions for a project
     */
    @GetMapping("/project/{projectId}")
    fun getSuggestionsByProject(@PathVariable projectId: UUID): List<MicroserviceSuggestionResponse> =
        suggestionRepository.findByAnalysisJobId(projectId)
            .map { it.toResponse() }

    /**
     * Get a specific microservice boundary suggestion by ID
     */
    @GetMapping("/{suggestionId}")
    fun getSuggestionById(@PathVariable suggestionId: UUID): ResponseEntity<MicroserviceSuggestionDetailResponse> =
        suggestionRepository.findById(suggestionId)
            .map { ResponseEntity.ok(it.toDetailResponse()) }
            .orElse(ResponseEntity.notFound().build())

    /**
     * Get suggestions by algorithm type
     */
    @GetMapping("/project/{projectId}/algorithm/{algorithm}")
    fun getSuggestionByAlgorithm(
        @PathVariable projectId: UUID,
        @PathVariable algorithm: BoundaryDetectionAlgorithm,
    ): ResponseEntity<MicroserviceSuggestionDetailResponse> {
        val suggestions = suggestionRepository.findByAnalysisJobId(projectId)
            .filter { it.algorithm == algorithm }

        return if (suggestions.isNotEmpty()) {
            ResponseEntity.ok(suggestions.first().toDetailResponse())
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Compare different algorithm suggestions for a project
     */
    @GetMapping("/project/{projectId}/compare")
    fun compareSuggestions(@PathVariable projectId: UUID): MicroserviceSuggestionComparisonResponse {
        val suggestions = suggestionRepository.findByAnalysisJobId(projectId)
            .map { it.toResponse() }

        return MicroserviceSuggestionComparisonResponse(
            projectId = projectId,
            suggestions = suggestions,
            bestSuggestion = suggestions.maxByOrNull { it.modularityScore }?.algorithm,
        )
    }
}

/**
 * Summary response for a microservice suggestion
 */
data class MicroserviceSuggestionResponse(
    val id: UUID,
    val algorithm: BoundaryDetectionAlgorithm,
    val modularityScore: Double,
    val numberOfServices: Int,
    val averageCohesion: Double,
    val averageCoupling: Double,
)

/**
 * Detailed response including all boundaries
 */
data class MicroserviceSuggestionDetailResponse(
    val id: UUID,
    val algorithm: BoundaryDetectionAlgorithm,
    val modularityScore: Double,
    val boundaries: List<ServiceBoundaryResponse>,
)

/**
 * Response for a service boundary
 */
data class ServiceBoundaryResponse(
    val id: UUID,
    val suggestedName: String,
    val metrics: BoundaryMetrics,
    val types: Set<String>,
)

/**
 * Response for comparing multiple suggestions
 */
data class MicroserviceSuggestionComparisonResponse(
    val projectId: UUID,
    val suggestions: List<MicroserviceSuggestionResponse>,
    val bestSuggestion: BoundaryDetectionAlgorithm?,
)

// Extension functions for mapping domain to response

private fun MicroserviceSuggestion.toResponse(): MicroserviceSuggestionResponse {
    val avgCohesion = boundaries.map { it.metrics.cohesion }.average().takeIf { !it.isNaN() } ?: 0.0
    val avgCoupling = boundaries.map { it.metrics.coupling.toDouble() }.average().takeIf { !it.isNaN() } ?: 0.0

    return MicroserviceSuggestionResponse(
        id = id,
        algorithm = algorithm,
        modularityScore = modularityScore,
        numberOfServices = boundaries.size,
        averageCohesion = avgCohesion,
        averageCoupling = avgCoupling,
    )
}

private fun MicroserviceSuggestion.toDetailResponse(): MicroserviceSuggestionDetailResponse =
    MicroserviceSuggestionDetailResponse(
        id = id,
        algorithm = algorithm,
        modularityScore = modularityScore,
        boundaries = boundaries.map { it.toResponse() },
    )

private fun ServiceBoundary.toResponse(): ServiceBoundaryResponse = ServiceBoundaryResponse(
    id = id,
    suggestedName = suggestedName,
    metrics = metrics,
    types = typeNames,
)
