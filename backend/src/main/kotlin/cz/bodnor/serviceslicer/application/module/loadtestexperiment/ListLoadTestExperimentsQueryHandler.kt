package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.ListLoadTestExperimentsQuery
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class ListLoadTestExperimentsQueryHandler(
    private val experimentReadService: LoadTestExperimentReadService,
) : QueryHandler<ListLoadTestExperimentsQuery.Result, ListLoadTestExperimentsQuery> {
    override val query = ListLoadTestExperimentsQuery::class

    override fun handle(query: ListLoadTestExperimentsQuery): ListLoadTestExperimentsQuery.Result {
        val experiments = experimentReadService.findAll()

        return ListLoadTestExperimentsQuery.Result(
            experiments = experiments.map {
                ListLoadTestExperimentsQuery.ExperimentSummary(
                    experimentId = it.id,
                    name = it.name,
                    description = it.description,
                    createdAt = it.createdAt,
                )
            },
        )
    }
}
