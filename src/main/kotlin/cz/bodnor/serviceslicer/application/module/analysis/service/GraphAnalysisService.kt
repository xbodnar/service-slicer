package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Service for analyzing the dependency graph stored in Neo4j
 */
@Service
class GraphAnalysisService(
    private val neo4jTemplate: Neo4jTemplate,
    private val classNodeRepository: ClassNodeRepository,
) {

    /**
     * Retrieves all class nodes for a project from Neo4j
     */
    fun getAllClassNodesForProject(projectId: UUID): List<ClassNode> {
        val query = """
            MATCH (n:ClassNode)
            WHERE n.projectId = ${'$'}projectId
            RETURN n
        """.trimIndent()

        return neo4jTemplate.findAll(query, mapOf("projectId" to projectId.toString()), ClassNode::class.java)
            .toList()
    }

    /**
     * Retrieves all class nodes with their references for a project
     */
    fun getClassNodesWithReferences(projectId: UUID): List<ClassNode> {
        val query = """
            MATCH (n:ClassNode)
            WHERE n.projectId = ${'$'}projectId
            OPTIONAL MATCH (n)-[r:DEPENDS_ON]->(ref:ClassNode)
            RETURN n, collect(r), collect(ref)
        """.trimIndent()

        return neo4jTemplate.findAll(query, mapOf("projectId" to projectId.toString()), ClassNode::class.java)
            .toList()
    }

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
        allNodes: List<ClassNode>,
    ): Int {
        val nodeMap = allNodes.associateBy { it.fullyQualifiedClassName }
        var count = 0

        nodeNames.forEach { nodeName ->
            val node = nodeMap[nodeName]
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
        allNodes: List<ClassNode>,
    ): Int {
        val nodeMap = allNodes.associateBy { it.fullyQualifiedClassName }
        var count = 0

        typeNames.forEach { typeName ->
            val node = nodeMap[typeName]
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
        allNodes: List<ClassNode>,
        internalReferencesCount: Int? = null,
        externalReferencesCount: Int? = null,
    ): Double {
        val internal = internalReferencesCount ?: countInternalReferences(typeNames, allNodes)
        val external = externalReferencesCount ?: countExternalReferences(typeNames, allNodes)
        val total = internal + external

        return if (total == 0) 0.0 else internal.toDouble() / total
    }
}
