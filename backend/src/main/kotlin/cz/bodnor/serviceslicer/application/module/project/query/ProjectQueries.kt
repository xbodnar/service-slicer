package cz.bodnor.serviceslicer.application.module.project.query

import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class ListProjectsQuery(val dummy: Unit = Unit) : Query<ListProjectsQuery.Result> {

    @Schema(name = "ListProjectsResult", description = "List of projects")
    data class Result(
        @Schema(description = "List of project summaries")
        val projects: List<ProjectSummary>,
    )

    @Schema(description = "Summary of a project")
    data class ProjectSummary(
        @Schema(description = "ID of the project")
        val projectId: UUID,
        @Schema(description = "Name of the project")
        val name: String,
        @Schema(description = "Base package name")
        val basePackageName: String,
        @Schema(description = "Creation timestamp")
        val createdAt: java.time.Instant,
    )
}

data class GetProjectQuery(val projectId: UUID) : Query<GetProjectQuery.Result> {

    @Schema(name = "GetProjectResult", description = "Detailed project information with analysis results")
    data class Result(
        @Schema(description = "ID of the project")
        val projectId: UUID,
        @Schema(description = "Name of the project")
        val name: String,
        @Schema(description = "Analysis job results")
        val analysisJobResult: AnalysisJobResult,
    )

    @Schema(description = "Analysis job results")
    data class AnalysisJobResult(
        @Schema(description = "Static analysis results")
        val staticAnalysis: StaticAnalysisResult,
    )

    @Schema(description = "Static analysis results including graph and decompositions")
    data class StaticAnalysisResult(
        @Schema(description = "Dependency graph summary")
        val dependencyGraph: GraphSummary,
        @Schema(description = "Label propagation algorithm results")
        val labelPropagationAlgorithm: DecompositionResults,
        @Schema(description = "Louvain algorithm results")
        val louvainAlgorithm: DecompositionResults,
        @Schema(description = "Leiden algorithm results")
        val leidenAlgorithm: DecompositionResults,
        @Schema(description = "Domain-driven decomposition results")
        val domainDrivenDecomposition: DecompositionResults,
        @Schema(description = "Actor-driven decomposition results")
        val actorDrivenDecomposition: DecompositionResults,
    )

    @Schema(description = "Summary of the dependency graph")
    data class GraphSummary(
        @Schema(description = "Number of nodes in the graph")
        val nodeCount: Int,
        @Schema(description = "Number of edges in the graph")
        val edgeCount: Int,
        @Schema(description = "List of class nodes")
        val nodes: List<ClassNodeDto>,
    )

    @Schema(description = "Class node in the dependency graph")
    data class ClassNodeDto(
        @Schema(description = "Simple class name")
        val simpleClassName: String,
        @Schema(description = "Fully qualified class name")
        val fullyQualifiedClassName: String,
        @Schema(description = "List of fully qualified names this class depends on")
        val dependencies: List<String>,
    )

    @Schema(description = "Decomposition results showing communities")
    data class DecompositionResults(
        @Schema(description = "Map of community ID to list of class fully qualified names")
        val communities: Map<String, List<String>>,
    )
}
