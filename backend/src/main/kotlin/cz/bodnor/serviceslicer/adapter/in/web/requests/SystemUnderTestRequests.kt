package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to add a system under test to a benchmark")
data class AddSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Whether this is the baseline system under test")
    val isBaseline: Boolean,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: GetBenchmarkQuery.DockerConfigDto,
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<GetBenchmarkQuery.DatabaseSeedConfigDto> = emptyList(),
) {

    fun toCommand(benchmarkId: UUID) = AddSystemUnderTestCommand(
        benchmarkId = benchmarkId,
        name = name,
        description = description,
        isBaseline = isBaseline,
        dockerConfig = dockerConfig.toDomain(),
        databaseSeedConfigs = databaseSeedConfigs.map { it.toDomain() },
    )
}

@Schema(description = "Request to update a system under test")
data class UpdateSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: GetBenchmarkQuery.DockerConfigDto,
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<GetBenchmarkQuery.DatabaseSeedConfigDto> = emptyList(),
) {
    fun toCommand(
        benchmarkId: UUID,
        sutId: UUID,
    ) = UpdateSystemUnderTestCommand(
        benchmarkId = benchmarkId,
        sutId = sutId,
        name = name,
        description = description,
        dockerConfig = dockerConfig.toDomain(),
        databaseSeedConfigs = databaseSeedConfigs.map { it.toDomain() },
    )
}
