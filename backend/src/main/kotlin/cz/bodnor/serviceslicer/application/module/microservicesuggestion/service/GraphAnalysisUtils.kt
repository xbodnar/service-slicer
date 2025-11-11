package cz.bodnor.serviceslicer.application.module.microservicesuggestion.service

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

/**
 * Utils class for analyzing the dependency graph stored in Neo4j
 */
class GraphAnalysisUtils {

    companion object {
        /**
         * Converts Neo4j graph to JGraphT directed graph for analysis
         */
        fun toJGraphT(classNodes: List<ClassNode>): Graph<String, DefaultEdge> {
            val graph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)

            // Add all vertices (using FQN as vertex identifier)
            classNodes.forEach { node ->
                graph.addVertex(node.fullyQualifiedClassName)
            }

            // Add edges for references
            classNodes.forEach { source ->
                source.dependencies.forEach { dependency ->
                    val target = dependency.target
                    if (graph.containsVertex(source.fullyQualifiedClassName) &&
                        graph.containsVertex(target.fullyQualifiedClassName)
                    ) {
                        graph.addEdge(source.fullyQualifiedClassName, target.fullyQualifiedClassName)
                    }
                }
            }

            return graph
        }

        /**
         * Counts internal references (within a set of nodes)
         */
        fun countInternalReferences(
            nodeNames: Set<String>,
            allNodes: Map<String, ClassNode>,
        ): Int {
            var count = 0

            nodeNames.forEach { nodeName ->
                val node = allNodes[nodeName]
                if (node != null) {
                    count += node.dependencies.count { it.target.fullyQualifiedClassName in nodeNames }
                }
            }

            return count
        }

        /**
         * Counts external references (from a set of nodes to outside that set)
         */
        fun countExternalReferences(
            typeNames: Set<String>,
            allNodes: Map<String, ClassNode>,
        ): Int {
            var count = 0

            typeNames.forEach { typeName ->
                val node = allNodes[typeName]
                if (node != null) {
                    count += node.dependencies.count { it.target.fullyQualifiedClassName !in typeNames }
                }
            }

            return count
        }

        /**
         * Calculates cohesion score for a group of types
         * Cohesion = internal references / (internal + external references)
         */
        fun calculateCohesion(
            typeNames: Set<String>,
            allNodes: Map<String, ClassNode>,
            internalReferencesCount: Int? = null,
            externalReferencesCount: Int? = null,
        ): Double {
            val internal = internalReferencesCount ?: countInternalReferences(typeNames, allNodes)
            val external = externalReferencesCount ?: countExternalReferences(typeNames, allNodes)
            val total = internal + external

            return if (total == 0) 0.0 else internal.toDouble() / total
        }

        /**
         * Counts how many distinct external communities this service depends on
         */
        fun countCouplingToOtherCommunities(nodes: List<ClassNode>): Int {
            val externalTypes = mutableSetOf<String>()

            nodes.forEach { node ->
                node.dependencies
                    .map { it.target }
                    .filter { it !in nodes }
                    .forEach { ref ->
                        externalTypes.add(ref.fullyQualifiedClassName)
                    }
            }

            return externalTypes.size
        }
    }
}
