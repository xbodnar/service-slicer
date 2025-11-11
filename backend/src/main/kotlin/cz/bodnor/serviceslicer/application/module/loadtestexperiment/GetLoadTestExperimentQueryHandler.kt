package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.GetLoadTestExperimentQuery
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTestRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetLoadTestExperimentQueryHandler(
    private val experimentReadService: LoadTestExperimentReadService,
    private val loadTestConfigReadService: LoadTestConfigReadService,
    private val systemUnderTestRepository: SystemUnderTestRepository,
) : QueryHandler<GetLoadTestExperimentQuery.Result, GetLoadTestExperimentQuery> {
    override val query = GetLoadTestExperimentQuery::class

    override fun handle(query: GetLoadTestExperimentQuery): GetLoadTestExperimentQuery.Result {
        val experiment = experimentReadService.getById(query.experimentId)
        val loadTestConfig = loadTestConfigReadService.getById(experiment.loadTestConfigId)
        val systemsUnderTest = systemUnderTestRepository.findByExperimentId(experiment.id)

        return GetLoadTestExperimentQuery.Result(
            experimentId = experiment.id,
            name = experiment.name,
            description = experiment.description,
            loadTestConfig = GetLoadTestExperimentQuery.LoadTestConfigDto(
                loadTestConfigId = loadTestConfig.id,
                name = loadTestConfig.name,
                openApiFileId = loadTestConfig.openApiFileId,
                behaviorModels = loadTestConfig.behaviorModels,
                operationalProfile = loadTestConfig.operationalProfile,
            ),
            systemsUnderTest = systemsUnderTest.map {
                GetLoadTestExperimentQuery.SystemUnderTestDto(
                    systemUnderTestId = it.id,
                    name = it.name,
                    composeFileId = it.composeFileId,
                    jarFileId = it.jarFileId,
                    description = it.description,
                    healthCheckPath = it.healthCheckPath,
                    appPort = it.appPort,
                    startupTimeoutSeconds = it.startupTimeoutSeconds,
                )
            },
            createdAt = experiment.createdAt,
            updatedAt = experiment.updatedAt,
        )
    }
}
