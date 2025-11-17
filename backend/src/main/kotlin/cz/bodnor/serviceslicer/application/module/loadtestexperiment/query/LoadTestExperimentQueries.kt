package cz.bodnor.serviceslicer.application.module.loadtestexperiment.query

import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalLoad
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "File information")
data class FileDto(
    @Schema(description = "ID of the file")
    val fileId: UUID,
    @Schema(description = "Name of the file")
    val filename: String,
    @Schema(description = "Size of the file in bytes")
    val fileSize: Long,
)

data class ListLoadTestExperimentsQuery(val dummy: Unit = Unit) : Query<ListLoadTestExperimentsQuery.Result> {

    @Schema(name = "ListLoadTestExperimentsResult", description = "List of load test experiments")
    data class Result(
        @Schema(description = "List of experiment summaries")
        val experiments: List<ExperimentSummary>,
    )

    @Schema(description = "Summary of a load test experiment")
    data class ExperimentSummary(
        @Schema(description = "ID of the experiment")
        val experimentId: UUID,
        @Schema(description = "Name of the experiment")
        val name: String,
        @Schema(description = "Description of the experiment")
        val description: String?,
        @Schema(description = "Creation timestamp")
        val createdAt: java.time.Instant,
    )
}

data class GetLoadTestExperimentQuery(val experimentId: UUID) : Query<GetLoadTestExperimentQuery.Result> {

    @Schema(name = "GetLoadTestExperimentResult", description = "Detailed load test experiment information")
    data class Result(
        @Schema(description = "ID of the experiment")
        val experimentId: UUID,
        @Schema(description = "Name of the experiment")
        val name: String,
        @Schema(description = "Description of the experiment")
        val description: String?,
        @Schema(description = "Load test configuration")
        val loadTestConfig: LoadTestConfigDto,
        @Schema(description = "List of systems under test")
        val systemsUnderTest: List<SystemUnderTestDto>,
        @Schema(description = "Creation timestamp")
        val createdAt: java.time.Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: java.time.Instant,
    )

    @Schema(description = "Load test configuration")
    data class LoadTestConfigDto(
        @Schema(description = "ID of the load test configuration")
        val loadTestConfigId: UUID,
        @Schema(description = "OpenAPI specification file")
        val openApiFile: FileDto,
        @Schema(description = "List of user behavior models")
        val behaviorModels: List<BehaviorModel>,
        @Schema(description = "Operational profile")
        val operationalProfile: List<OperationalLoad>,
    )

    @Schema(description = "System under test")
    data class SystemUnderTestDto(
        @Schema(description = "ID of the system under test")
        val systemUnderTestId: UUID,
        @Schema(description = "Name of the system under test")
        val name: String,
        @Schema(description = "Docker Compose file")
        val composeFile: FileDto,
        @Schema(description = "JAR file to test")
        val jarFile: FileDto,
        @Schema(description = "SQL seed file (optional)")
        val sqlSeedFile: FileDto?,
        @Schema(description = "Description of the system under test")
        val description: String?,
        @Schema(description = "Health check endpoint path")
        val healthCheckPath: String,
        @Schema(description = "Application port")
        val appPort: Int,
        @Schema(description = "Startup timeout in seconds")
        val startupTimeoutSeconds: Long,
    )
}
