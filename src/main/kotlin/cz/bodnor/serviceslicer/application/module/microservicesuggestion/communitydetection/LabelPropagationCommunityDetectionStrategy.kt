package cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection

import cz.bodnor.serviceslicer.application.module.microservicesuggestion.service.GraphAnalysisUtils
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryDetectionAlgorithm
import org.jgrapht.alg.clustering.LabelPropagationClustering
import org.jgrapht.graph.AsUndirectedGraph
import kotlin.math.sqrt

class LabelPropagationCommunityDetectionStrategy(
    private val maxIterations: Int = 200, // Zero means no limit
    private val minCommunitySize: Int = 10,
    private val targetServiceCount: Int? = null,
) : CommunityDetectionStrategy {

    override val algorithm = BoundaryDetectionAlgorithm.COMMUNITY_DETECTION_LABEL_PROPAGATION

    override fun detect(classNodes: List<ClassNode>): CommunityDetectionStrategy.Result {
        // Auto-calculate target service count if not specified
        val effectiveTargetCount = targetServiceCount ?: calculateTargetServiceCount(classNodes.size, minCommunitySize)

        // Convert to JGraphT graph
        val directedGraph = GraphAnalysisUtils.toJGraphT(classNodes)

        // Convert to undirected for community detection
        val undirectedGraph = AsUndirectedGraph(directedGraph)

        // Run label propagation clustering
        val clustering = LabelPropagationClustering(undirectedGraph, maxIterations)
        val communities = clustering.clustering

        // Convert communities to service boundaries
        val classNodeMap = classNodes.associateBy { it.fullyQualifiedClassName }
        val initialBoundaries = communities.clusters.map { community ->
            val communityNodes = community.mapNotNull { classNodeMap[it] }

            val typeNames = communityNodes.map { it.fullyQualifiedClassName }.toSet()

            CommunityDetectionStrategy.Result.Community(
                nodes = typeNames,
            )
        }.filter { it.nodes.isNotEmpty() } // Filter out empty communities

        // Post-process: merge small communities
        val finalCommunities = mergeSmallBoundaries(
            communities = initialBoundaries,
            classNodes = classNodes,
            minSize = minCommunitySize,
            targetCount = effectiveTargetCount,
        )

        return CommunityDetectionStrategy.Result(
            communities = finalCommunities,
            directedGraph = directedGraph,
        )
    }

    /**
     * Merges small boundaries into larger ones based on dependency strength
     */
    private fun mergeSmallBoundaries(
        communities: List<CommunityDetectionStrategy.Result.Community>,
        classNodes: List<ClassNode>,
        minSize: Int,
        targetCount: Int,
    ): List<CommunityDetectionStrategy.Result.Community> {
        val mutableCommunities = communities.toMutableList()
        val classNodeMap = classNodes.associateBy { it.fullyQualifiedClassName }

        // Keep merging until we meet both criteria or can't merge anymore
        while (mutableCommunities.size > targetCount || mutableCommunities.any { it.nodes.size < minSize }) {
            // Find the smallest boundary
            val smallestBoundary = mutableCommunities.minByOrNull { it.nodes.size } ?: break

            // If the smallest boundary is already large enough and we've reached target count, stop
            if (smallestBoundary.nodes.size >= minSize && mutableCommunities.size <= targetCount) {
                break
            }

            // Find the best merge candidate based on dependency strength
            val bestMatch = findBestMergeCandidate(smallestBoundary, mutableCommunities, classNodeMap)

            if (bestMatch != null) {
                // Merge smallest into best match
                val merged = CommunityDetectionStrategy.Result.Community(
                    nodes = listOf(smallestBoundary, bestMatch).flatMap { it.nodes }.toSet(),
                )

                mutableCommunities.remove(smallestBoundary)
                mutableCommunities.remove(bestMatch)
                mutableCommunities.add(merged)
            } else {
                // Can't find a merge candidate, stop to avoid infinite loop
                break
            }
        }

        return mutableCommunities
    }

    /**
     * Finds the best boundary to merge with, based on dependency coupling
     */
    private fun findBestMergeCandidate(
        community: CommunityDetectionStrategy.Result.Community,
        allCommunities: List<CommunityDetectionStrategy.Result.Community>,
        classNodeMap: Map<String, ClassNode>,
    ): CommunityDetectionStrategy.Result.Community? {
        val sourceTypes = community.nodes

        // Calculate coupling strength to each other boundary
        val couplingScores = allCommunities
            .filter { it != community }
            .map { targetBoundary ->
                val targetTypes = targetBoundary.nodes
                val couplingStrength = calculateCouplingStrength(sourceTypes, targetTypes, classNodeMap)
                targetBoundary to couplingStrength
            }
            .filter { it.second > 0 } // Only consider boundaries with some coupling

        // Return the boundary with highest coupling (or largest if no coupling exists)
        return couplingScores.maxByOrNull { it.second }?.first
            ?: allCommunities.filter { it != community }.maxByOrNull { it.nodes.size }
    }

    /**
     * Calculates coupling strength between two sets of types (bidirectional reference count)
     */
    private fun calculateCouplingStrength(
        sourceNodes: Set<String>,
        targetNodes: Set<String>,
        classNodeMap: Map<String, ClassNode>,
    ): Int {
        var strength = 0

        // Count references from source to target
        sourceNodes.forEach { sourceType ->
            val node = classNodeMap[sourceType]
            if (node != null) {
                strength += node.dependencies.count { it.target.fullyQualifiedClassName in targetNodes }
            }
        }

        // Count references from target to source
        targetNodes.forEach { targetType ->
            val node = classNodeMap[targetType]
            if (node != null) {
                strength += node.dependencies.count { it.target.fullyQualifiedClassName in sourceNodes }
            }
        }

        return strength
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
        val rawTarget = sqrt(totalClasses.toDouble() / minCommunitySize).toInt()

        // Bound between 2 and 15 services
        return rawTarget.coerceIn(2, 15)
    }
}
