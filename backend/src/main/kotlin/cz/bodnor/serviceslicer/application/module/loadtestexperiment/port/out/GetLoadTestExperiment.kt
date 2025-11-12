package cz.bodnor.serviceslicer.application.module.loadtestexperiment.port.out

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.GetLoadTestExperimentQuery
import java.util.UUID

interface GetLoadTestExperiment {
    operator fun invoke(experimentId: UUID): GetLoadTestExperimentQuery.Result
}
