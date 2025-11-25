package cz.bodnor.serviceslicer.application.module.benchmark.query

import cz.bodnor.serviceslicer.domain.benchmark.BehaviorModel
import cz.bodnor.serviceslicer.domain.benchmark.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.benchmark.DockerConfig
import cz.bodnor.serviceslicer.domain.benchmark.OperationalLoad
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
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

data class ListBenchmarksQuery(val dummy: Unit = Unit) : Query<ListBenchmarksQuery.Result> {

    @Schema(name = "ListBenchmarksResult", description = "List of benchmarks")
    data class Result(
        @Schema(description = "List of benchmark summaries")
        val benchmarks: List<BenchmarkSummary>,
    )

    @Schema(description = "Summary of a benchmark")
    data class BenchmarkSummary(
        @Schema(description = "ID of the benchmark")
        val benchmarkId: UUID,
        @Schema(description = "Name of the benchmark")
        val name: String,
        @Schema(description = "Description of the benchmark")
        val description: String?,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
    )
}

data class GetBenchmarkQuery(val benchmarkId: UUID) : Query<GetBenchmarkQuery.Result> {

    @Schema(name = "GetBenchmarkResult", description = "Detailed benchmark information")
    data class Result(
        @Schema(description = "ID of the benchmark")
        val benchmarkId: UUID,
        @Schema(description = "Name of the benchmark")
        val name: String,
        @Schema(description = "Description of the benchmark")
        val description: String?,
        @Schema(description = "Load test configuration")
        val loadTestConfig: LoadTestConfigDto,
        @Schema(description = "List of systems under test")
        val systemsUnderTest: List<SystemUnderTestDto>,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: Instant,
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
        @Schema(description = "Description of the system under test")
        val description: String?,
        @Schema(description = "Whether this is the baseline system under test")
        val isBaseline: Boolean,
        @Schema(description = "Docker configuration")
        val dockerConfig: DockerConfigDto,
        @Schema(description = "Database seed configuration (optional)")
        val databaseSeedConfig: DatabaseSeedConfigDto?,
        @Schema(description = "Result of the last validation run (optional)")
        val validationResult: ValidationResult?,
    )

    data class DockerConfigDto(
        @field:Schema(description = "ID of the Docker Compose file")
        val composeFile: FileDto,
        @field:Schema(description = "Health check endpoint path")
        val healthCheckPath: String,
        @field:Schema(description = "Application port")
        val appPort: Int,
        @field:Schema(description = "Startup timeout in seconds")
        val startupTimeoutSeconds: Long,
    ) {
        fun toDomain() = DockerConfig(
            composeFileId = composeFile.fileId,
            healthCheckPath = healthCheckPath,
            appPort = appPort,
            startupTimeoutSeconds = startupTimeoutSeconds,
        )
    }

    data class DatabaseSeedConfigDto(
        @field:Schema(description = "ID of the SQL seed file")
        val sqlSeedFile: FileDto,
        @field:Schema(description = "Database container name in docker-compose")
        val dbContainerName: String,
        @field:Schema(description = "Database port inside container")
        val dbPort: Int,
        @field:Schema(description = "Database name")
        val dbName: String,
        @field:Schema(description = "Database username")
        val dbUsername: String,
    ) {
        fun toDomain() = DatabaseSeedConfig(
            sqlSeedFileId = sqlSeedFile.fileId,
            dbContainerName = dbContainerName,
            dbPort = dbPort,
            dbName = dbName,
            dbUsername = dbUsername,
        )
    }
}
