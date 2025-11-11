package cz.bodnor.serviceslicer.application.module.loadtestexperiment.query

import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.util.UUID

data class FileDto(
    val fileId: UUID,
    val filename: String,
    val fileSize: Long,
)

data class ListLoadTestExperimentsQuery(val dummy: Unit = Unit) : Query<ListLoadTestExperimentsQuery.Result> {

    data class Result(
        val experiments: List<ExperimentSummary>,
    )

    data class ExperimentSummary(
        val experimentId: UUID,
        val name: String,
        val description: String?,
        val createdAt: java.time.Instant,
    )
}

data class GetLoadTestExperimentQuery(val experimentId: UUID) : Query<GetLoadTestExperimentQuery.Result> {

    data class Result(
        val experimentId: UUID,
        val name: String,
        val description: String?,
        val loadTestConfig: LoadTestConfigDto,
        val systemsUnderTest: List<SystemUnderTestDto>,
        val createdAt: java.time.Instant,
        val updatedAt: java.time.Instant,
    )

    data class LoadTestConfigDto(
        val loadTestConfigId: UUID,
        val openApiFile: FileDto,
        val behaviorModels: List<BehaviorModel>,
        val operationalProfile: OperationalProfile?,
    )

    data class SystemUnderTestDto(
        val systemUnderTestId: UUID,
        val name: String,
        val composeFile: FileDto,
        val jarFile: FileDto,
        val description: String?,
        val healthCheckPath: String,
        val appPort: Int,
        val startupTimeoutSeconds: Long,
    )
}
