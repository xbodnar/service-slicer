package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.query.GetDecompositionJobSummaryQuery
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidateReadService
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

        return GetDecompositionJobSummaryQuery.Result(
            decompositionJob = decompositionJob,
            dependencyGraph = graphNodes,
            decompositionCandidates = decompositionCandidates,
        )
    }
}
