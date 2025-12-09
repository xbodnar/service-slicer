package cz.bodnor.serviceslicer.adapter.out.neo4j

import cz.bodnor.serviceslicer.application.module.decomposition.service.CommunityDetectionAlgorithm
import cz.bodnor.serviceslicer.application.module.decomposition.service.RunCommunityDetectionAlgorithm
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import cz.bodnor.serviceslicer.domain.graph.ClassNodeReadService
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.stereotype.Service
import java.util.UUID

private const val ALGO_GDS_LEIDEN = "gds.leiden.write"
private const val ALGO_GDS_LOUVAIN = "gds.louvain.write"
private const val ALGO_GDS_LABEL_PROPAGATION = "gds.labelPropagation.write"

@Service
class RunCommunityDetectionAlgorithmNeo4j(
    private val neo4j: Neo4jClient,
    private val classNodeReadService: ClassNodeReadService,
) : RunCommunityDetectionAlgorithm {
    private val label = "ClassNode"
    private val relType = "DEPENDS_ON"

    override fun invoke(
        decompositionJobId: UUID,
        algorithm: CommunityDetectionAlgorithm,
    ): RunCommunityDetectionAlgorithm.Result {
        val result = when (algorithm) {
            CommunityDetectionAlgorithm.LEIDEN -> runLeiden(decompositionJobId)
            CommunityDetectionAlgorithm.LOUVAIN -> runLouvain(decompositionJobId)
            CommunityDetectionAlgorithm.LABEL_PROPAGATION -> runLabelPropagation(decompositionJobId)
        }

        val modularity = result["modularity"] as? Double

        val classNodes = classNodeReadService.findAllByDecompositionJobId(decompositionJobId)
        val communities = when (algorithm) {
            CommunityDetectionAlgorithm.LEIDEN -> classNodes.groupBy { it.communityLeiden }
            CommunityDetectionAlgorithm.LOUVAIN -> classNodes.groupBy { it.communityLouvain }
            CommunityDetectionAlgorithm.LABEL_PROPAGATION -> classNodes.groupBy { it.communityLabelPropagation }
        }
            .filterKeys { it != null }
            .mapKeys { (k, _) -> k!!.toString() }

        return RunCommunityDetectionAlgorithm.Result(
            communities = communities,
            modularity = modularity,
        )
    }

    private fun runLeiden(decompositionJobId: UUID): Map<String, Any?> = runCommunityAlgo(
        decompositionJobId = decompositionJobId,
        algoQualifiedName = ALGO_GDS_LEIDEN,
        writeProperty = ClassNode::communityLeiden.name,
    )

    private fun runLouvain(decompositionJobId: UUID): Map<String, Any?> = runCommunityAlgo(
        decompositionJobId = decompositionJobId,
        algoQualifiedName = ALGO_GDS_LOUVAIN,
        writeProperty = ClassNode::communityLouvain.name,
    )

    fun runLabelPropagation(decompositionJobId: UUID): Map<String, Any?> = runCommunityAlgo(
        decompositionJobId = decompositionJobId,
        algoQualifiedName = ALGO_GDS_LABEL_PROPAGATION,
        writeProperty = ClassNode::communityLabelPropagation.name,
    )

    /**
     * Projects the subgraph for the given decompositionJobId, runs the chosen community algorithm (write mode),
     * returns YIELDed metrics, and drops the in-memory graph.
     */
    private fun runCommunityAlgo(
        decompositionJobId: UUID,
        algoQualifiedName: String,
        writeProperty: String,
    ): Map<String, Any?> {
        val graphName = "g_${UUID.randomUUID()}"

        projectGraph(graphName, decompositionJobId)

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
     * Cypher projection restricted to (:ClassNode {decompositionJobId}) and [:DEPENDS_ON].
     * - Node ids use internal id(n) for GDS.
     * - Relationship weight taken from r.weight (default 1.0).
     */
    private fun projectGraph(
        graphName: String,
        decompositionJobId: UUID,
    ) {
        // Use a Cypher projection with explicit filtering and weight default.
        val q = """
        MATCH (a:ClassNode {decompositionJobId: $${"pid"}})-[r:DEPENDS_ON]->(b:ClassNode {decompositionJobId: $${"pid"}})
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
            .bind(decompositionJobId.toString()).to("pid")
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
