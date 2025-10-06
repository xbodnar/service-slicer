package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryDetectionAlgorithm
import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.analysis.suggestion.MicroserviceSuggestion
import cz.bodnor.serviceslicer.domain.analysis.suggestion.ServiceBoundary
import org.jgrapht.Graph
import org.jgrapht.alg.clustering.LabelPropagationClustering
import org.jgrapht.graph.AsUndirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Detects microservice boundaries using community detection algorithms
 *
 * Uses Label Propagation algorithm to identify densely connected communities in the dependency graph.
 * These communities represent groups of classes that work closely together and could form microservices.
 */
@Component
class CommunityDetectionBoundaryDetector(
    private val graphAnalysisService: GraphAnalysisService,
) {

    /**
     * Generates microservice boundary suggestions using community detection
     *
     * @param analysisJobId The analysis job ID
     * @param classNodes All class nodes in the project
     * @param maxIterations Maximum iterations for label propagation (default: 100)
     * @param minCommunitySize Minimum classes per service (default: 20)
     * @param targetServiceCount Target number of services (null = auto-calculate from size)
     */
    operator fun invoke(
        analysisJobId: UUID,
        classNodes: List<ClassNode>,
        maxIterations: Int = 100,
        minCommunitySize: Int = 20,
        targetServiceCount: Int? = null,
    ): MicroserviceSuggestion {
        // Auto-calculate target service count if not specified
        val effectiveTargetCount = targetServiceCount ?: calculateTargetServiceCount(classNodes.size, minCommunitySize)
        // Convert to JGraphT graph
        val directedGraph = graphAnalysisService.toJGraphT(classNodes)

        // Convert to undirected for community detection
        val undirectedGraph = AsUndirectedGraph(directedGraph)

        // Run label propagation clustering
        val clustering = LabelPropagationClustering(undirectedGraph, maxIterations)
        val communities = clustering.clustering

        // Convert communities to service boundaries
        val classNodeMap = classNodes.associateBy { it.fullyQualifiedClassName }
        val initialBoundaries = communities.clusters.mapIndexed { index, community ->
            val communityNodes = community.mapNotNull { classNodeMap[it] }

            CommunityBoundary(
                index + 1,
                communityNodes,
            )
        }.filter { it.nodes.isNotEmpty() } // Filter out empty communities

        // Post-process: merge small communities and consolidate duplicates
        val processedBoundaries = postProcessBoundaries(
            boundaries = initialBoundaries,
            classNodes = classNodes,
            minCommunitySize = minCommunitySize,
            targetServiceCount = effectiveTargetCount,
        )

        // Calculate overall modularity score
        val modularityScore = calculateModularityScore(processedBoundaries, directedGraph)

        val suggestion = MicroserviceSuggestion(
            analysisJobId = analysisJobId,
            algorithm = BoundaryDetectionAlgorithm.COMMUNITY_DETECTION,
            modularityScore = modularityScore,
        )

        processedBoundaries.forEach { suggestion.addBoundary(it) }

        return suggestion
    }

    /**
     * Creates a service boundary from a community of types
     */
    private fun createServiceBoundary(
        communityId: Int,
        serviceNodes: List<ClassNode>,
        allNodes: List<ClassNode>,
    ): ServiceBoundary {
        if (serviceNodes.isEmpty()) {
            // Return empty boundary for filtering
            return ServiceBoundary(
                suggestedName = "Empty Community $communityId",
                metrics = BoundaryMetrics(0, 0.0, 0, 0, 0),
            )
        }

        val typeNames = serviceNodes.map { it.fullyQualifiedClassName }.toSet()

        val internalDeps = graphAnalysisService.countInternalReferences(typeNames, allNodes)
        val externalDeps = graphAnalysisService.countExternalReferences(typeNames, allNodes)
        val cohesion = graphAnalysisService.calculateCohesion(typeNames, allNodes, internalDeps, externalDeps)

        // Count coupling to other services
        val couplingCount = countCouplingToOtherCommunities(serviceNodes, typeNames)

        val metrics = BoundaryMetrics(
            size = serviceNodes.size,
            cohesion = cohesion,
            coupling = couplingCount,
            internalDependencies = internalDeps,
            externalDependencies = externalDeps,
        )

        val suggestedName = deriveServiceName(serviceNodes, communityId)
        val boundary = ServiceBoundary(
            suggestedName = suggestedName,
            metrics = metrics,
        )

        typeNames.forEach { boundary.addType(it) }

        return boundary
    }

    /**
     * Counts how many distinct external communities this service depends on
     */
    private fun countCouplingToOtherCommunities(
        types: List<ClassNode>,
        internalTypeNames: Set<String>,
    ): Int {
        val externalTypes = mutableSetOf<String>()

        types.forEach { type ->
            type.dependencies
                .map { it.target }
                .filter { it.fullyQualifiedClassName !in internalTypeNames }
                .forEach { ref ->
                    externalTypes.add(ref.fullyQualifiedClassName)
                }
        }

        return externalTypes.size
    }

    /**
     * Derives a service name from the most common package in the community
     */
    private fun deriveServiceName(
        types: List<ClassNode>,
        communityId: Int,
    ): String {
        // Find most common package prefix
        val packageCounts = types
            .groupBy { extractPackagePrefix(it.fullyQualifiedClassName) }
            .mapValues { it.value.size }

        val dominantPackage = packageCounts.maxByOrNull { it.value }?.key

        return if (dominantPackage != null && dominantPackage.isNotEmpty()) {
            val lastSegment = dominantPackage.split(".").lastOrNull() ?: "Unknown"
            "${lastSegment.replaceFirstChar { it.uppercase() }} Service"
        } else {
            "Service Cluster $communityId"
        }
    }

    /**
     * Extracts package prefix (all but last segment)
     */
    private fun extractPackagePrefix(fullyQualifiedName: String): String {
        val parts = fullyQualifiedName.split(".")
        return if (parts.size > 1) {
            parts.dropLast(1).joinToString(".")
        } else {
            ""
        }
    }

    /**
     * Post-processes boundaries to:
     * 1. Merge small boundaries into larger ones based on strongest dependencies
     * 2. Keep merging until we reach target service count or min size threshold
     * 3. Map CommunityBoundaries to ServiceBoundaries
     */
    private fun postProcessBoundaries(
        boundaries: List<CommunityBoundary>,
        classNodes: List<ClassNode>,
        minCommunitySize: Int,
        targetServiceCount: Int,
    ): List<ServiceBoundary> {
        // Merge small boundaries into larger neighbors
        val mergedBoundaries = mergeSmallBoundaries(boundaries, classNodes, minCommunitySize, targetServiceCount)

        return mergedBoundaries.map {
            createServiceBoundary(
                communityId = it.communityId,
                serviceNodes = it.nodes,
                allNodes = classNodes,
            )
        }
    }

    /**
     * Merges small boundaries into larger ones based on dependency strength
     */
    private fun mergeSmallBoundaries(
        boundaries: List<CommunityBoundary>,
        classNodes: List<ClassNode>,
        minSize: Int,
        targetCount: Int,
    ): List<CommunityBoundary> {
        val mutableBoundaries = boundaries.toMutableList()
        val classNodeMap = classNodes.associateBy { it.fullyQualifiedClassName }

        // Keep merging until we meet both criteria or can't merge anymore
        while (mutableBoundaries.size > targetCount || mutableBoundaries.any { it.nodes.size < minSize }) {
            // Find the smallest boundary
            val smallestBoundary = mutableBoundaries.minByOrNull { it.nodes.size } ?: break

            // If the smallest boundary is already large enough and we've reached target count, stop
            if (smallestBoundary.nodes.size >= minSize && mutableBoundaries.size <= targetCount) {
                break
            }

            // Find the best merge candidate based on dependency strength
            val bestMatch = findBestMergeCandidate(smallestBoundary, mutableBoundaries, classNodeMap)

            if (bestMatch != null) {
                // Merge smallest into best match
                val merged = mergeBoundaries(
                    listOf(smallestBoundary, bestMatch),
                    classNodes,
                )

                mutableBoundaries.remove(smallestBoundary)
                mutableBoundaries.remove(bestMatch)
                mutableBoundaries.add(merged)
            } else {
                // Can't find a merge candidate, stop to avoid infinite loop
                break
            }
        }

        return mutableBoundaries
    }

    /**
     * Finds the best boundary to merge with, based on dependency coupling
     */
    private fun findBestMergeCandidate(
        sourceBoundary: CommunityBoundary,
        allBoundaries: List<CommunityBoundary>,
        classNodeMap: Map<String, ClassNode>,
    ): CommunityBoundary? {
        val sourceTypes = sourceBoundary.typeNames()

        // Calculate coupling strength to each other boundary
        val couplingScores = allBoundaries
            .filter { it != sourceBoundary }
            .map { targetBoundary ->
                val targetTypes = targetBoundary.typeNames()
                val couplingStrength = calculateCouplingStrength(sourceTypes, targetTypes, classNodeMap)
                targetBoundary to couplingStrength
            }
            .filter { it.second > 0 } // Only consider boundaries with some coupling

        // Return the boundary with highest coupling (or largest if no coupling exists)
        return couplingScores.maxByOrNull { it.second }?.first
            ?: allBoundaries.filter { it != sourceBoundary }.maxByOrNull { it.nodes.size }
    }

    /**
     * Calculates coupling strength between two sets of types (bidirectional reference count)
     */
    private fun calculateCouplingStrength(
        sourceTypes: Set<String>,
        targetTypes: Set<String>,
        classNodeMap: Map<String, ClassNode>,
    ): Int {
        var strength = 0

        // Count references from source to target
        sourceTypes.forEach { sourceType ->
            val node = classNodeMap[sourceType]
            if (node != null) {
                strength += node.dependencies.count { it.target.fullyQualifiedClassName in targetTypes }
            }
        }

        // Count references from target to source
        targetTypes.forEach { targetType ->
            val node = classNodeMap[targetType]
            if (node != null) {
                strength += node.dependencies.count { it.target.fullyQualifiedClassName in sourceTypes }
            }
        }

        return strength
    }

    /**
     * Merges multiple boundaries into a single boundary
     */
    private fun mergeBoundaries(
        boundaries: List<CommunityBoundary>,
        allNodes: List<ClassNode>,
    ): CommunityBoundary {
        // Collect all types from all boundaries
        val allTypes = boundaries.flatMap { it.typeNames() }.toSet()
        val classNodeMap = allNodes.associateBy { it.fullyQualifiedClassName }
        val communityNodes = allTypes.mapNotNull { classNodeMap[it] }

        // Recalculate metrics for the merged boundary
        val internalDeps = graphAnalysisService.countInternalReferences(allTypes, allNodes)
        val externalDeps = graphAnalysisService.countExternalReferences(allTypes, allNodes)
        val cohesion = graphAnalysisService.calculateCohesion(allTypes, allNodes, internalDeps, externalDeps)
        val couplingCount = countCouplingToOtherCommunities(communityNodes, allTypes)

        val metrics = BoundaryMetrics(
            size = allTypes.size,
            cohesion = cohesion,
            coupling = couplingCount,
            internalDependencies = internalDeps,
            externalDependencies = externalDeps,
        )

        val boundary = CommunityBoundary(
            communityId = boundaries.first().communityId,
            nodes = allNodes,
        )

        return boundary
    }

    /**
     * Dynamically calculates target service count based on codebase size
     *
     * Strategy:
     * - Small codebases (< 50 classes): 2-3 services
     * - Medium codebases (50-200 classes): 3-6 services
     * - Large codebases (200-500 classes): 6-10 services
     * - Very large codebases (500+ classes): 10-15 services
     *
     * Formula: sqrt(totalClasses / minCommunitySize) bounded by [2, 15]
     */
    private fun calculateTargetServiceCount(
        totalClasses: Int,
        minCommunitySize: Int,
    ): Int {
        if (totalClasses == 0) return 2

        // Square root scaling: grows slowly with size
        val rawTarget = kotlin.math.sqrt(totalClasses.toDouble() / minCommunitySize).toInt()

        // Bound between 2 and 15 services
        return rawTarget.coerceIn(2, 15)
    }

    /**
     * Calculates modularity score using standard graph modularity formula
     * Q = (1/2m) * sum[ (internal edges - expected) ]
     */
    private fun calculateModularityScore(
        boundaries: List<ServiceBoundary>,
        graph: Graph<String, DefaultEdge>,
    ): Double {
        if (boundaries.isEmpty() || graph.edgeSet().isEmpty()) return 0.0

        val totalEdges = graph.edgeSet().size.toDouble()
        var modularity = 0.0

        boundaries.forEach { boundary ->
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

data class CommunityBoundary(
    val communityId: Int,
    val nodes: List<ClassNode>,
) {
    fun typeNames(): Set<String> = nodes.map { it.fullyQualifiedClassName }.toSet()
}
