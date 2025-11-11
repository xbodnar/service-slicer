package cz.bodnor.serviceslicer.application.module.microservicesuggestion.service

import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.CommunityDetectionStrategy
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode

/**
 * Reassigns hub nodes to communities after initial detection
 *
 * Hub nodes (utilities, common classes) are assigned based on coupling strength
 * to detected communities rather than participating in community detection directly.
 */
object HubNodeReassigner {

    /**
     * Reassignment strategy for hub nodes
     */
    enum class Strategy {
        /**
         * Assign to the community with strongest coupling
         */
        STRONGEST_COUPLING,

        /**
         * Replicate hub node across all communities that use it
         */
        REPLICATE,

        /**
         * Create a dedicated "shared" service for all hub nodes
         */
        SHARED_SERVICE,
    }

    /**
     * Reassigns hub nodes to communities
     *
     * @param communities Initial communities (without hub nodes)
     * @param hubNodes Hub nodes to reassign
     * @param allNodes All class nodes (for dependency lookup)
     * @param strategy Reassignment strategy
     * @return Updated communities with hub nodes assigned
     */
    fun reassignHubNodes(
        communities: List<CommunityDetectionStrategy.Result.Community>,
        hubNodes: List<ClassNode>,
        allNodes: List<ClassNode>,
        strategy: Strategy = Strategy.STRONGEST_COUPLING,
    ): List<CommunityDetectionStrategy.Result.Community> {
        if (hubNodes.isEmpty()) return communities

        return when (strategy) {
            Strategy.STRONGEST_COUPLING -> assignByStrongestCoupling(communities, hubNodes, allNodes)
            Strategy.REPLICATE -> replicateAcrossCommunities(communities, hubNodes, allNodes)
            Strategy.SHARED_SERVICE -> createSharedService(communities, hubNodes)
        }
    }

    /**
     * Assigns each hub node to the community it's most strongly coupled with
     */
    private fun assignByStrongestCoupling(
        communities: List<CommunityDetectionStrategy.Result.Community>,
        hubNodes: List<ClassNode>,
        allNodes: List<ClassNode>,
    ): List<CommunityDetectionStrategy.Result.Community> {
        val classNodeMap = allNodes.associateBy { it.fullyQualifiedClassName }
        val updatedCommunities = communities.map { it.copy() }.toMutableList()

        for (hubNode in hubNodes) {
            val hubName = hubNode.fullyQualifiedClassName

            // Calculate coupling strength to each community
            val couplingScores = updatedCommunities.map { community ->
                val coupling = calculateCouplingToCommunity(hubName, community.nodes, classNodeMap)
                community to coupling
            }

            // Assign to community with strongest coupling
            val bestMatch = couplingScores.maxByOrNull { it.second }?.first

            if (bestMatch != null) {
                val index = updatedCommunities.indexOf(bestMatch)
                updatedCommunities[index] = CommunityDetectionStrategy.Result.Community(
                    id = bestMatch.id,
                    nodes = bestMatch.nodes + hubName,
                )
            } else {
                // If no coupling found, assign to largest community
                val largest = updatedCommunities.maxByOrNull { it.nodes.size }
                if (largest != null) {
                    val index = updatedCommunities.indexOf(largest)
                    updatedCommunities[index] = CommunityDetectionStrategy.Result.Community(
                        id = largest.id,
                        nodes = largest.nodes + hubName,
                    )
                }
            }
        }

        return updatedCommunities
    }

    /**
     * Replicates hub nodes across all communities that use them
     *
     * This reflects the reality that shared utilities may need to be deployed
     * with multiple services.
     */
    private fun replicateAcrossCommunities(
        communities: List<CommunityDetectionStrategy.Result.Community>,
        hubNodes: List<ClassNode>,
        allNodes: List<ClassNode>,
    ): List<CommunityDetectionStrategy.Result.Community> {
        val classNodeMap = allNodes.associateBy { it.fullyQualifiedClassName }
        val updatedCommunities = communities.map { it.copy() }.toMutableList()

        for (hubNode in hubNodes) {
            val hubName = hubNode.fullyQualifiedClassName

            // Find all communities that reference this hub
            val dependentCommunities = updatedCommunities.mapIndexed { index, community ->
                val hasDependency = community.nodes.any { nodeName ->
                    val node = classNodeMap[nodeName]
                    node?.dependencies?.any { dep ->
                        dep.target.fullyQualifiedClassName == hubName
                    } ?: false
                }
                if (hasDependency) index else null
            }.filterNotNull()

            // Add hub to all dependent communities
            for (index in dependentCommunities) {
                val community = updatedCommunities[index]
                updatedCommunities[index] = CommunityDetectionStrategy.Result.Community(
                    id = community.id,
                    nodes = community.nodes + hubName,
                )
            }

            // If no dependencies found, add to largest community
            if (dependentCommunities.isEmpty()) {
                val largest = updatedCommunities.withIndex().maxByOrNull { it.value.nodes.size }
                if (largest != null) {
                    updatedCommunities[largest.index] = CommunityDetectionStrategy.Result.Community(
                        id = largest.value.id,
                        nodes = largest.value.nodes + hubName,
                    )
                }
            }
        }

        return updatedCommunities
    }

    /**
     * Creates a dedicated "shared" service containing all hub nodes
     */
    private fun createSharedService(
        communities: List<CommunityDetectionStrategy.Result.Community>,
        hubNodes: List<ClassNode>,
    ): List<CommunityDetectionStrategy.Result.Community> {
        val sharedService = CommunityDetectionStrategy.Result.Community(
            nodes = hubNodes.map { it.fullyQualifiedClassName }.toSet(),
        )

        return communities + sharedService
    }

    /**
     * Calculates bidirectional coupling strength between a hub node and a community
     */
    private fun calculateCouplingToCommunity(
        hubNodeName: String,
        communityNodes: Set<String>,
        classNodeMap: Map<String, ClassNode>,
    ): Int {
        var coupling = 0

        // Count references from hub to community
        val hubNode = classNodeMap[hubNodeName]
        if (hubNode != null) {
            coupling += hubNode.dependencies.count { dep ->
                dep.target.fullyQualifiedClassName in communityNodes
            }
        }

        // Count references from community to hub
        communityNodes.forEach { nodeName ->
            val node = classNodeMap[nodeName]
            if (node != null) {
                coupling += node.dependencies.count { dep ->
                    dep.target.fullyQualifiedClassName == hubNodeName
                }
            }
        }

        return coupling
    }
}
