package cz.bodnor.serviceslicer.application.module.microservicesuggestion.service

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

private const val ALGO_GDS_LEIDEN = "gds.leiden.write"
private const val ALGO_GDS_LOUVAIN = "gds.louvain.write"
private const val ALGO_GDS_LABEL_PROPAGATION = "gds.labelPropagation.write"

@Service
class CommunityDetectionService(
    private val neo4j: Neo4jClient,
) {
    private val label = "ClassNode"
    private val relType = "DEPENDS_ON"

    @Transactional
    fun runLeiden(projectId: UUID): Map<String, Any?> = runCommunityAlgo(
        projectId = projectId,
        algoQualifiedName = ALGO_GDS_LEIDEN,
        writeProperty = ClassNode::communityLeiden.name,
    )

    @Transactional
    fun runLouvain(projectId: UUID): Map<String, Any?> = runCommunityAlgo(
        projectId = projectId,
        algoQualifiedName = ALGO_GDS_LOUVAIN,
        writeProperty = ClassNode::communityLouvain.name,
    )

    @Transactional
    fun runLabelPropagation(projectId: UUID): Map<String, Any?> = runCommunityAlgo(
        projectId = projectId,
        algoQualifiedName = ALGO_GDS_LABEL_PROPAGATION,
        writeProperty = ClassNode::communityLabelPropagation.name,
    )

    /**
     * Projects the subgraph for the given projectId, runs the chosen community algorithm (write mode),
     * returns YIELDed metrics, and drops the in-memory graph.
     */
    private fun runCommunityAlgo(
        projectId: UUID,
        algoQualifiedName: String,
        writeProperty: String,
    ): Map<String, Any?> {
        val graphName = "g_${UUID.randomUUID()}"

        projectGraph(graphName, projectId)

        return try {
            val config = mapOf(
                "writeProperty" to writeProperty,
                "relationshipWeightProperty" to "weight",
                // Optional: tune for speed
                // "concurrency" to 8
            )

            val runQuery = """
                CALL $algoQualifiedName($$graphName, $$config)
                YIELD *
            """.trimIndent()
                .replace("$$graphName", "\$graphName")
                .replace("$$config", "\$config")

            neo4j.query(runQuery)
                .bind(graphName).to("graphName")
                .bind(config).to("config")
                .fetch()
                .one()
                .orElseThrow { IllegalStateException("No result from community detection algorithm") }
        } finally {
            dropGraphQuietly(graphName)
        }
    }

    /**
     * Cypher projection restricted to (:ClassNode {projectId}) and [:DEPENDS_ON].
     * - Node ids use internal id(n) for GDS.
     * - Relationship weight taken from r.weight (default 1.0).
     */
    private fun projectGraph(
        graphName: String,
        projectId: UUID,
    ) {
        // Use a Cypher projection with explicit filtering and weight default.
        val q = """
        MATCH (a:ClassNode {projectId: $${"pid"}})-[r:DEPENDS_ON]->(b:ClassNode {projectId: $${"pid"}})
        WITH a, b, coalesce(r.weight, 1.0) AS weight
        RETURN gds.graph.project(
          $${"graphName"},
          a,                                   // source node
          b,                                   // target node
          {                                    // dataConfig
            relationshipProperties: { weight: weight },
            relationshipType: 'DEPENDS_ON'
          },
          {                                    // configuration
            undirectedRelationshipTypes: ['DEPENDS_ON']
          }
        ) AS g
        """.trimIndent()

        neo4j.query(q)
            .bind(graphName).to("graphName")
            .bind(projectId.toString()).to("pid")
            .run()
    }

    private fun dropGraphQuietly(graphName: String) {
        try {
            neo4j.query("CALL gds.graph.drop(\$graphName, false)")
                .bind(graphName).to("graphName")
                .run()
        } catch (_: Exception) {
            // ignore if already dropped
        }
    }
}
