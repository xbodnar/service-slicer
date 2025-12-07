package cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection

import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionMethod
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import java.util.UUID

interface CommunityDetectionStrategy {

    data class Result(
        val communities: List<Community>,
        val directedGraph: Graph<String, DefaultEdge>,
    ) {
        data class Community(
            // Fully qualified class names
            val id: UUID = UUID.randomUUID(),
            val nodes: Set<String>,
        )
    }

    fun detect(classNodes: List<ClassNode>): Result

    val method: DecompositionMethod
}
