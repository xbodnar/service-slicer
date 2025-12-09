package cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection

import cz.bodnor.serviceslicer.application.module.decomposition.communitydetection.CommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.decomposition.service.GraphAnalysisUtils
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionMethod
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import org.jgrapht.Graph
import org.jgrapht.graph.AsUndirectedGraph
import org.jgrapht.graph.DefaultEdge

/**
 * Louvain community detection strategy
 *
 * Implements the Louvain method for community detection, which optimizes modularity
 * through hierarchical optimization. More deterministic and balanced than Label Propagation.
 *
 * Algorithm:
 * 1. Phase 1: Local modularity optimization - each node tries to join neighboring communities
 * 2. Phase 2: Network aggregation - merge communities into super-nodes
 * 3. Repeat until modularity stops improving
 *
 * @param minCommunitySize Minimum number of classes per microservice
 * @param targetServiceCount Target number of services (null = auto-calculate)
 * @param resolution Resolution parameter (higher = more communities, default = 1.0)
 */
class LouvainCommunityDetectionStrategy(
    private val minCommunitySize: Int = 10,
    private val targetServiceCount: Int? = null,
    private val resolution: Double = 1.0,
) : CommunityDetectionStrategy {

    override val method = DecompositionMethod.COMMUNITY_DETECTION_LOUVAIN

    override fun detect(classNodes: List<ClassNode>): CommunityDetectionStrategy.Result {
        val directedGraph = GraphAnalysisUtils.toJGraphT(classNodes)
        val undirectedGraph = AsUndirectedGraph(directedGraph)

        // Initialize: each node in its own community
        val nodeToCommunity = mutableMapOf<String, Int>()
        classNodes.forEachIndexed { index, node ->
            nodeToCommunity[node.fullyQualifiedClassName] = index
        }

        // Calculate total edge weight (for modularity)
        val totalEdgeWeight = undirectedGraph.edgeSet().size.toDouble()

        var improved = true
        var iteration = 0
        val maxIterations = 100

        while (improved && iteration < maxIterations) {
            improved = false
            iteration++

            // Phase 1: Local optimization
            for (node in classNodes.shuffled()) { // Shuffle for randomization
                val nodeName = node.fullyQualifiedClassName
                val currentCommunity = nodeToCommunity[nodeName] ?: continue

                // Try moving node to each neighbor's community
                val neighborCommunities = getNeighborCommunities(nodeName, undirectedGraph, nodeToCommunity)

                if (neighborCommunities.isEmpty()) continue

                val bestCommunity = findBestCommunity(
                    node = nodeName,
                    currentCommunity = currentCommunity,
                    neighborCommunities = neighborCommunities,
                    nodeToCommunity = nodeToCommunity,
                    graph = undirectedGraph,
                    totalEdgeWeight = totalEdgeWeight,
                )

                if (bestCommunity != currentCommunity) {
                    nodeToCommunity[nodeName] = bestCommunity
                    improved = true
                }
            }
        }

        // Group nodes by community
        val communities = nodeToCommunity.entries
            .groupBy { it.value }
            .values
            .map { communityNodes ->
                CommunityDetectionStrategy.Result.Community(
                    nodes = communityNodes.map { it.key }.toSet(),
                )
            }
            .filter { it.nodes.isNotEmpty() }

        // Post-process: merge small communities
        val finalCommunities = mergeSmallCommunities(
            communities = communities,
            classNodes = classNodes,
            minSize = minCommunitySize,
            targetCount = targetServiceCount ?: calculateTargetServiceCount(classNodes.size),
        )

        return CommunityDetectionStrategy.Result(
            communities = finalCommunities,
            directedGraph = directedGraph,
        )
    }

    private fun getNeighborCommunities(
        node: String,
        graph: Graph<String, DefaultEdge>,
        nodeToCommunity: Map<String, Int>,
    ): Set<Int> = graph.edgesOf(node)
        .mapNotNull { edge ->
            val neighbor = if (graph.getEdgeSource(edge) == node) {
                graph.getEdgeTarget(edge)
            } else {
                graph.getEdgeSource(edge)
            }
            nodeToCommunity[neighbor]
        }
        .toSet()

    private fun findBestCommunity(
        node: String,
        currentCommunity: Int,
        neighborCommunities: Set<Int>,
        nodeToCommunity: Map<String, Int>,
        graph: Graph<String, DefaultEdge>,
        totalEdgeWeight: Double,
    ): Int {
        val currentModularity = calculateModularityDelta(
            node = node,
            targetCommunity = currentCommunity,
            nodeToCommunity = nodeToCommunity,
            graph = graph,
            totalEdgeWeight = totalEdgeWeight,
        )

        var bestCommunity = currentCommunity
        var bestModularity = currentModularity

        for (targetCommunity in neighborCommunities) {
            if (targetCommunity == currentCommunity) continue

            val modularity = calculateModularityDelta(
                node = node,
                targetCommunity = targetCommunity,
                nodeToCommunity = nodeToCommunity,
                graph = graph,
                totalEdgeWeight = totalEdgeWeight,
            )

            if (modularity > bestModularity) {
                bestModularity = modularity
                bestCommunity = targetCommunity
            }
        }

        return bestCommunity
    }

    /**
     * Calculates the modularity gain from moving node to target community
     */
    private fun calculateModularityDelta(
        node: String,
        targetCommunity: Int,
        nodeToCommunity: Map<String, Int>,
        graph: Graph<String, DefaultEdge>,
        totalEdgeWeight: Double,
    ): Double {
        // Count edges from node to target community
        val edgesToCommunity = graph.edgesOf(node).count { edge ->
            val neighbor = if (graph.getEdgeSource(edge) == node) {
                graph.getEdgeTarget(edge)
            } else {
                graph.getEdgeSource(edge)
            }
            nodeToCommunity[neighbor] == targetCommunity
        }

        // Node degree
        val nodeDegree = graph.edgesOf(node).size

        // Community total degree (sum of degrees of all nodes in community)
        val communityDegree = nodeToCommunity.entries
            .filter { it.value == targetCommunity }
            .sumOf { graph.edgesOf(it.key).size }

        // Modularity delta formula
        val m = totalEdgeWeight
        if (m == 0.0) return 0.0

        return (edgesToCommunity / m) - resolution * ((nodeDegree * communityDegree) / (2.0 * m * m))
    }

    private fun mergeSmallCommunities(
        communities: List<CommunityDetectionStrategy.Result.Community>,
        classNodes: List<ClassNode>,
        minSize: Int,
        targetCount: Int,
    ): List<CommunityDetectionStrategy.Result.Community> {
        val mutableCommunities = communities.toMutableList()
        val classNodeMap = classNodes.associateBy { it.fullyQualifiedClassName }

        while (mutableCommunities.size > targetCount || mutableCommunities.any { it.nodes.size < minSize }) {
            val smallestCommunity = mutableCommunities.minByOrNull { it.nodes.size } ?: break

            if (smallestCommunity.nodes.size >= minSize && mutableCommunities.size <= targetCount) {
                break
            }

            val bestMatch = findBestMergeCandidate(smallestCommunity, mutableCommunities, classNodeMap)

            if (bestMatch != null) {
                val merged = CommunityDetectionStrategy.Result.Community(
                    nodes = (smallestCommunity.nodes + bestMatch.nodes),
                )

                mutableCommunities.remove(smallestCommunity)
                mutableCommunities.remove(bestMatch)
                mutableCommunities.add(merged)
            } else {
                break
            }
        }

        return mutableCommunities
    }

    private fun findBestMergeCandidate(
        community: CommunityDetectionStrategy.Result.Community,
        allCommunities: List<CommunityDetectionStrategy.Result.Community>,
        classNodeMap: Map<String, ClassNode>,
    ): CommunityDetectionStrategy.Result.Community? {
        val couplingScores = allCommunities
            .filter { it != community }
            .map { targetCommunity ->
                val couplingStrength = calculateCouplingStrength(community.nodes, targetCommunity.nodes, classNodeMap)
                targetCommunity to couplingStrength
            }
            .filter { it.second > 0 }

        return couplingScores.maxByOrNull { it.second }?.first
            ?: allCommunities.filter { it != community }.maxByOrNull { it.nodes.size }
    }

    private fun calculateCouplingStrength(
        sourceNodes: Set<String>,
        targetNodes: Set<String>,
        classNodeMap: Map<String, ClassNode>,
    ): Int {
        var strength = 0

        sourceNodes.forEach { sourceType ->
            val node = classNodeMap[sourceType]
            if (node != null) {
                strength += node.dependencies.count { it.target.fullyQualifiedClassName in targetNodes }
            }
        }

        targetNodes.forEach { targetType ->
            val node = classNodeMap[targetType]
            if (node != null) {
                strength += node.dependencies.count { it.target.fullyQualifiedClassName in sourceNodes }
            }
        }

        return strength
    }

    private fun calculateTargetServiceCount(totalClasses: Int): Int {
        if (totalClasses == 0) return 2
        val rawTarget = kotlin.math.sqrt(totalClasses.toDouble() / minCommunitySize).toInt()
        return rawTarget.coerceIn(2, 15)
    }
}
