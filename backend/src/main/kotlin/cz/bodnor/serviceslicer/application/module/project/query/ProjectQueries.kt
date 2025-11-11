package cz.bodnor.serviceslicer.application.module.project.query

import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.util.UUID

data class ListProjectsQuery(val dummy: Unit = Unit) : Query<ListProjectsQuery.Result> {

    data class Result(
        val projects: List<ProjectSummary>,
    )

    data class ProjectSummary(
        val projectId: UUID,
        val name: String,
        val basePackageName: String,
        val createdAt: java.time.Instant,
    )
}

data class GetProjectQuery(val projectId: UUID) : Query<GetProjectQuery.Result> {

    data class Result(
        val projectId: UUID,
        val name: String,
        val analysisJobResult: AnalysisJobResult,
    )

    data class AnalysisJobResult(
        val staticAnalysis: StaticAnalysisResult,
    )

    data class StaticAnalysisResult(
        val dependencyGraph: GraphSummary,
        val labelPropagationAlgorithm: DecompositionResults,
        val louvainAlgorithm: DecompositionResults,
        val leidenAlgorithm: DecompositionResults,
        val domainDrivenDecomposition: DecompositionResults,
        val actorDrivenDecomposition: DecompositionResults,
    )

    data class GraphSummary(
        val nodeCount: Int,
        val edgeCount: Int,
    )

    data class DecompositionResults(
//        val metrics: CommunityMetrics? = null,        // e.g., modularity, iterations
        val communities: Map<String, List<String>>, // (communityId -> class FQN)
    )
}
