package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkCommand
import cz.bodnor.serviceslicer.application.module.benchmark.command.UpdateBenchmarkCommand
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a benchmark")
data class CreateBenchmarkRequest(
    @field:Schema(description = "Name of the benchmark", example = "Baseline Performance Test")
    val name: String,
    @field:Schema(
        description = "Description of the benchmark",
        example = "Testing baseline performance with monolithic architecture",
    )
    val description: String? = null,
    @field:Schema(description = "Load test configuration")
    val benchmarkConfig: BenchmarkConfig,
    @field:Schema(description = "List of systems under test to compare")
    val systemsUnderTest: List<CreateSystemUnderTestRequest>,
) {

    fun toCommand() = CreateBenchmarkCommand(
        name = name,
        description = description,
        benchmarkConfig = benchmarkConfig,
        systemsUnderTest = systemsUnderTest.map { it.toCommand() },
    )
}

@Schema(description = "Request to create a system under test")
data class CreateSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Whether this is the baseline system under test")
    val isBaseline: Boolean,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: DockerConfig,
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) {
    fun toCommand() = CreateBenchmarkCommand.UpdateSystemUnderTest(
        name = name,
        description = description,
        isBaseline = isBaseline,
        dockerConfig = dockerConfig,
        databaseSeedConfigs = databaseSeedConfigs,
    )
}

@Schema(description = "Request to update a benchmark")
data class UpdateBenchmarkRequest(
    @field:Schema(description = "Name of the benchmark", example = "Baseline Performance Test")
    val name: String,
    @field:Schema(
        description = "Description of the benchmark",
        example = "Testing baseline performance with monolithic architecture",
    )
    val description: String? = null,
    @field:Schema(description = "Load test configuration")
    val benchmarkConfig: BenchmarkConfig,
) {
    fun toCommand(benchmarkId: UUID) = UpdateBenchmarkCommand(
        benchmarkId = benchmarkId,
        name = name,
        description = description,
        benchmarkConfig = benchmarkConfig,
    )
}
