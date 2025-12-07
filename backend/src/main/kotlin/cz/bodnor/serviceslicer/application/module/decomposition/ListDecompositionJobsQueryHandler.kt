package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.query.ListDecompositionJobsQuery
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class ListDecompositionJobsQueryHandler(
    private val decompositionJobReadService: DecompositionJobReadService,
) : QueryHandler<Page<DecompositionJob>, ListDecompositionJobsQuery> {
    override val query = ListDecompositionJobsQuery::class

    override fun handle(query: ListDecompositionJobsQuery): Page<DecompositionJob> =
        decompositionJobReadService.findAll(query.toPageable())
}
