package cz.bodnor.serviceslicer.application.module.microservicesuggestion.service

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode

/**
 * Preprocesses dependency graphs before community detection
 *
 * Identifies and handles hub nodes (highly connected utility/common classes)
 * that can skew community detection results.
 */
object GraphPreprocessor {

    /**
     * Result of graph preprocessing
     *
     * @param regularNodes Nodes to use for community detection
     * @param hubNodes Nodes to assign after community detection (high-degree utilities)
     */
    data class PreprocessResult(
        val regularNodes: List<ClassNode>,
        val hubNodes: List<ClassNode>,
    )

    /**
     * Identifies hub nodes based on degree percentile
     *
     * Hub nodes are classes with degree (in + out) exceeding the specified percentile.
     * These are typically:
     * - Common DTOs/Value Objects referenced everywhere
     * - Utility classes
     * - Framework/infrastructure classes
     * - Exception/error classes
     *
     * @param classNodes All nodes in the graph
     * @param hubPercentile Percentile threshold for hub detection (e.g., 0.9 = top 10%)
     * @return Preprocessing result with regular and hub nodes separated
     */
    fun filterHubNodes(
        classNodes: List<ClassNode>,
        hubPercentile: Double = 0.90,
    ): PreprocessResult {
        require(hubPercentile in 0.0..1.0) { "Hub percentile must be between 0.0 and 1.0" }

        if (classNodes.isEmpty()) {
            return PreprocessResult(emptyList(), emptyList())
        }

        // Calculate node degrees (incoming + outgoing edges)
        val nodeDegrees = classNodes.associateWith { node ->
            calculateNodeDegree(node, classNodes)
        }

        // Calculate degree threshold
        val sortedDegrees = nodeDegrees.values.sorted()
        val thresholdIndex = (sortedDegrees.size * hubPercentile).toInt().coerceIn(0, sortedDegrees.size - 1)
        val degreeThreshold = sortedDegrees[thresholdIndex]

        // Separate hub nodes from regular nodes
        val hubNodes = mutableListOf<ClassNode>()
        val regularNodes = mutableListOf<ClassNode>()

        nodeDegrees.forEach { (node, degree) ->
            if (degree >= degreeThreshold && degree > 0) {
                hubNodes.add(node)
            } else {
                regularNodes.add(node)
            }
        }

        // If we've filtered out too many nodes, don't filter at all
        // (this prevents degenerate cases where most nodes are considered hubs)
        if (regularNodes.size < classNodes.size * 0.5) {
            return PreprocessResult(classNodes, emptyList())
        }

        return PreprocessResult(regularNodes, hubNodes)
    }

    /**
     * Applies package-based filtering to identify cross-cutting concerns
     *
     * Identifies classes in common packages (util, common, shared, config, etc.)
     * as potential hub nodes.
     *
     * @param classNodes All nodes
     * @param commonPackagePatterns Regex patterns for common packages
     * @return Preprocessing result
     */
    fun filterByPackage(
        classNodes: List<ClassNode>,
        commonPackagePatterns: List<Regex> = DEFAULT_COMMON_PACKAGE_PATTERNS,
    ): PreprocessResult {
        val hubNodes = classNodes.filter { node ->
            commonPackagePatterns.any { pattern ->
                pattern.containsMatchIn(node.fullyQualifiedClassName)
            }
        }

        val regularNodes = classNodes - hubNodes.toSet()

        return PreprocessResult(regularNodes, hubNodes)
    }

    /**
     * Combines degree-based and package-based filtering
     *
     * @param classNodes All nodes
     * @param hubPercentile Degree percentile threshold
     * @param includeCommonPackages Whether to also filter common packages
     * @return Preprocessing result
     */
    fun filterHubNodesAdvanced(
        classNodes: List<ClassNode>,
        hubPercentile: Double = 0.90,
        includeCommonPackages: Boolean = true,
    ): PreprocessResult {
        // Start with degree-based filtering
        val degreeResult = filterHubNodes(classNodes, hubPercentile)

        if (!includeCommonPackages) {
            return degreeResult
        }

        // Add package-based filtering
        val packageResult = filterByPackage(degreeResult.regularNodes)

        return PreprocessResult(
            regularNodes = packageResult.regularNodes,
            hubNodes = degreeResult.hubNodes + packageResult.hubNodes,
        )
    }

    /**
     * Calculates the total degree of a node (in-degree + out-degree)
     */
    private fun calculateNodeDegree(
        node: ClassNode,
        allNodes: List<ClassNode>,
    ): Int {
        // Out-degree: dependencies from this node
        val outDegree = node.dependencies.size

        // In-degree: dependencies to this node from others
        val inDegree = allNodes.count { otherNode ->
            otherNode.dependencies.any { dep ->
                dep.target.fullyQualifiedClassName == node.fullyQualifiedClassName
            }
        }

        return inDegree + outDegree
    }

    private val DEFAULT_COMMON_PACKAGE_PATTERNS = listOf(
        Regex("\\.util(s)?\\."),
        Regex("\\.common\\."),
        Regex("\\.shared\\."),
        Regex("\\.config\\."),
        Regex("\\.core\\."),
        Regex("\\.infrastructure\\."),
        Regex("\\.exception(s)?\\."),
        Regex("\\.constant(s)?\\."),
        Regex("\\.helper(s)?\\."),
    )
}
