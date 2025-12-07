package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.query.GetDecompositionJobSummaryQuery
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidateReadService
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import cz.bodnor.serviceslicer.domain.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetDecompositionJobSummaryQueryHandler(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val decompositionCandidateReadService: DecompositionCandidateReadService,
    private val classNodeRepository: ClassNodeRepository,
) : QueryHandler<GetDecompositionJobSummaryQuery.Result, GetDecompositionJobSummaryQuery> {

    override val query = GetDecompositionJobSummaryQuery::class

    override fun handle(query: GetDecompositionJobSummaryQuery): GetDecompositionJobSummaryQuery.Result {
        val decompositionJob = decompositionJobReadService.getById(query.decompositionJobId)

        val graphNodes = classNodeRepository.findAllByDecompositionJobId(decompositionJob.id)

        val decompositionCandidates = decompositionCandidateReadService.findAllByDecompositionJobId(decompositionJob.id)

        val dependencyGraph = GetDecompositionJobSummaryQuery.GraphSummary(
            nodeCount = graphNodes.size,
            edgeCount = graphNodes.sumOf { it.dependencies.size },
            nodes = graphNodes.map { node ->
                GetDecompositionJobSummaryQuery.ClassNodeDto(
                    simpleClassName = node.simpleClassName,
                    fullyQualifiedClassName = node.fullyQualifiedClassName,
                    dependencies = node.dependencies.map { it.target.fullyQualifiedClassName },
                )
            },
        )

        return GetDecompositionJobSummaryQuery.Result(
            decompositionJob = decompositionJob,
            dependencyGraph = dependencyGraph,
            decompositionResults = GetDecompositionJobSummaryQuery.DecompositionResults(
                labelPropagation = graphNodes.communities { it.communityLabelPropagation.toString() },
                louvain = graphNodes.communities { it.communityLouvain.toString() },
                leiden = graphNodes.communities { it.communityLeiden.toString() },
                domainDriven = graphNodes.communities { it.domainDrivenClusterId },
                actorDriven = graphNodes.communities { it.actorDrivenClusterId },
            ),
        )
    }

    private fun List<ClassNode>.communities(keySelector: (ClassNode) -> String?): Map<String, List<String>> =
        this.filter { node -> keySelector(node) != null }
            .groupBy { keySelector(it)!! }
            .mapValues { it.value.map { node -> node.fullyQualifiedClassName } }
            .toMap()
}
