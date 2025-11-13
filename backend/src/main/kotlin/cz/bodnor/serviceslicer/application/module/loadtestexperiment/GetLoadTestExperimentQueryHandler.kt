package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.port.out.GetLoadTestExperiment
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.GetLoadTestExperimentQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetLoadTestExperimentQueryHandler(
    private val getLoadTestExperiment: GetLoadTestExperiment,
) : QueryHandler<GetLoadTestExperimentQuery.Result, GetLoadTestExperimentQuery> {
    override val query = GetLoadTestExperimentQuery::class

    override fun handle(query: GetLoadTestExperimentQuery): GetLoadTestExperimentQuery.Result =
        getLoadTestExperiment(query.experimentId)
}
