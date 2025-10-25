package cz.bodnor.serviceslicer.application.module.graph.build

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import java.util.UUID

interface BuildDependencyGraph {

    data class Graph(
        val nodes: List<ClassNode>,
    )

    operator fun invoke(projectId: UUID): Graph
}
