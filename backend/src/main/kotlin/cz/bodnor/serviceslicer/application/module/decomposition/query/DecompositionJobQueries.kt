package cz.bodnor.serviceslicer.application.module.decomposition.query

import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

data class ListDecompositionJobsQuery(
    val page: Int = 0,
    val size: Int = 10,
) : Query<Page<DecompositionJob>> {
    fun toPageable() = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTimestamp"))
}

data class GetDecompositionJobSummaryQuery(val decompositionJobId: UUID) :
    Query<GetDecompositionJobSummaryQuery.Result> {
    data class Result(
        val decompositionJob: DecompositionJob,
        val dependencyGraph: GraphSummary,
        val decompositionResults: DecompositionResults,
    )

    data class DecompositionResults(
        val labelPropagation: Map<String, List<String>>,
        val louvain: Map<String, List<String>>,
        val leiden: Map<String, List<String>>,
        val domainDriven: Map<String, List<String>>,
        val actorDriven: Map<String, List<String>>,
    )

    data class GraphSummary(
        val nodeCount: Int,
        val edgeCount: Int,
        val nodes: List<ClassNodeDto>,
    )

    data class ClassNodeDto(
        val simpleClassName: String,
        val fullyQualifiedClassName: String,
        val dependencies: List<String>,
    )
}
