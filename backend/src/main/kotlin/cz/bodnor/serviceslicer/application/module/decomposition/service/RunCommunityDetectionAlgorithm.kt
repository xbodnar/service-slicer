package cz.bodnor.serviceslicer.application.module.decomposition.service

import cz.bodnor.serviceslicer.domain.graph.ClassNode
import java.util.UUID

interface RunCommunityDetectionAlgorithm {

    data class Result(
        val communities: Map<String, List<ClassNode>>,
        val modularity: Double?,
    )

    operator fun invoke(
        decompositionJobId: UUID,
        algorithm: CommunityDetectionAlgorithm,
    ): Result
}

enum class CommunityDetectionAlgorithm {
    LEIDEN,
    LOUVAIN,
    LABEL_PROPAGATION,
}
