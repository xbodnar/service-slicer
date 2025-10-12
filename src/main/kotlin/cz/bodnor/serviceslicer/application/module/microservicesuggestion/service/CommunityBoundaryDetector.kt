package cz.bodnor.serviceslicer.application.module.microservicesuggestion.service

import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.CommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.port.out.SuggestServiceBoundaryNames
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.analysis.suggestion.MicroserviceSuggestion
import cz.bodnor.serviceslicer.domain.analysis.suggestion.ServiceBoundary
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.springframework.stereotype.Component
import java.util.UUID
import kotlin.collections.forEach

/**
 * Detects microservice boundaries using community detection algorithms
 *
 * Uses Label Propagation algorithm to identify densely connected communities in the dependency graph.
 * These communities represent groups of classes that work closely together and could form microservices.
 */
@Component
class CommunityBoundaryDetector(
    private val suggestServiceBoundaryNames: SuggestServiceBoundaryNames,
) {

    /**
     * Generates microservice boundary suggestions using community detection
     *
     * @param analysisJobId The analysis job ID
     * @param classNodes All class nodes in the project
     * @param communityDetectionStrategy
     */
    operator fun invoke(
        analysisJobId: UUID,
        classNodes: List<ClassNode>,
        communityDetectionStrategy: CommunityDetectionStrategy,
    ): MicroserviceSuggestion {
        val communityDetectionResult = communityDetectionStrategy.detect(classNodes)

        val serviceBoundaryNames = suggestServiceBoundaryNames(communityDetectionResult.communities)
            .serviceNames.associateBy { it.id }

        // Calculate overall modularity score
        val serviceBoundaries = communityDetectionResult.communities.map {
            val suggestedServiceName = serviceBoundaryNames[it.id]?.serviceName
                ?: throw IllegalStateException("Service name not found for community ID: ${it.id}")
            createServiceBoundary(
                community = it,
                allNodes = classNodes.associateBy { node -> node.fullyQualifiedClassName },
                suggestedName = suggestedServiceName,
            )
        }
        val modularityScore = calculateModularityScore(serviceBoundaries, communityDetectionResult.directedGraph)

        val suggestion = MicroserviceSuggestion(
            analysisJobId = analysisJobId,
            algorithm = communityDetectionStrategy.algorithm,
            modularityScore = modularityScore,
        )

        serviceBoundaries.forEach { suggestion.addBoundary(it) }

        return suggestion
    }

    fun createServiceBoundary(
        community: CommunityDetectionStrategy.Result.Community,
        allNodes: Map<String, ClassNode>,
        suggestedName: String,
    ): ServiceBoundary {
        require(community.nodes.isNotEmpty()) { "Community must have at least one node" }

        val communityNodes = community.nodes.mapNotNull { allNodes[it] }

        val internalDeps = GraphAnalysisUtils.countInternalReferences(community.nodes, allNodes)
        val externalDeps = GraphAnalysisUtils.countExternalReferences(community.nodes, allNodes)
        val cohesion = GraphAnalysisUtils.calculateCohesion(community.nodes, allNodes, internalDeps, externalDeps)

        // Count coupling to other services
        val couplingCount = GraphAnalysisUtils.countCouplingToOtherCommunities(communityNodes)

        val metrics = BoundaryMetrics(
            size = communityNodes.size,
            cohesion = cohesion,
            coupling = couplingCount,
            internalDependencies = internalDeps,
            externalDependencies = externalDeps,
        )

        val boundary = ServiceBoundary(
            suggestedName = suggestedName,
            metrics = metrics,
        )

        community.nodes.forEach { boundary.addType(it) }

        return boundary
    }

    /**
     * Calculates modularity score using standard graph modularity formula
     * Q = (1/2m) * sum[ (internal edges - expected) ]
     */
    private fun calculateModularityScore(
        serviceBoundaries: List<ServiceBoundary>,
        graph: Graph<String, DefaultEdge>,
    ): Double {
        if (serviceBoundaries.isEmpty() || graph.edgeSet().isEmpty()) return 0.0

        val totalEdges = graph.edgeSet().size.toDouble()
        var modularity = 0.0

        serviceBoundaries.forEach { boundary ->
            val internal = boundary.metrics.internalDependencies
            val external = boundary.metrics.externalDependencies
            val communityDegree = internal + external

            // Expected edges if randomly distributed
            val expected = (communityDegree * communityDegree) / (2.0 * totalEdges)

            modularity += (internal - expected)
        }

        return modularity / (2.0 * totalEdges)
    }
}
