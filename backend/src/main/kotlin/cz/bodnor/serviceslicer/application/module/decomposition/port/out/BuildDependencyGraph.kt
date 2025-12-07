package cz.bodnor.serviceslicer.application.module.decomposition.port.out

import cz.bodnor.serviceslicer.domain.graph.ClassNode
import java.util.UUID

interface BuildDependencyGraph {

    data class Graph(
        val nodes: List<ClassNode>,
    )

    operator fun invoke(decompositionJobId: UUID): Graph
}
