package cz.bodnor.serviceslicer.application.module.project.query

import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJobStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.util.UUID

data class GetProjectQuery(val projectId: UUID) : Query<GetProjectQuery.Result> {

    data class Result(
        val projectId: UUID,
        val name: String,
        val analysisJobResult: AnalysisJobResult,
    )

    data class AnalysisJobResult(
        val analysisJobId: UUID,
        val status: AnalysisJobStatus,
        val staticAnalysis: StaticAnalysisResult,
    )

    data class StaticAnalysisResult(
        val dependencyGraph: GraphSummary,
        val labelPropagationAlgorithm: DecompositionResults,
        val louvainAlgorithm: DecompositionResults,
        val leidenAlgorithm: DecompositionResults,
    )

    data class GraphSummary(
        val nodeCount: Int,
        val edgeCount: Int,
    )

    data class DecompositionResults(
//        val metrics: CommunityMetrics? = null,        // e.g., modularity, iterations
        val communities: Map<Long, List<String>>, // (communityId -> class FQN)
    )
}
