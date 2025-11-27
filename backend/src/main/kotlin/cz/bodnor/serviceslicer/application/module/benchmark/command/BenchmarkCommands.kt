package cz.bodnor.serviceslicer.application.module.benchmark.command

import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateBenchmarkCommand(
    val name: String,
    val description: String? = null,
    val benchmarkConfig: BenchmarkConfig,
    val systemsUnderTest: List<CreateBenchmarkCommand.UpdateSystemUnderTest>,
) : Command<CreateBenchmarkCommand.Result> {

    @Schema(name = "CreateBenchmarkResult", description = "Result of creating a benchmark")
    data class Result(
        @field:Schema(description = "ID of the created benchmark")
        val benchmarkId: UUID,
    )

    data class UpdateSystemUnderTest(
        val name: String,
        val description: String? = null,
        val isBaseline: Boolean,
        val dockerConfig: DockerConfig,
        val databaseSeedConfig: DatabaseSeedConfig? = null,
    ) {
        fun toDomain(benchmarkId: UUID) = SystemUnderTest(
            benchmarkId = benchmarkId,
            name = name,
            description = description,
            isBaseline = isBaseline,
            dockerConfig = dockerConfig,
            databaseSeedConfig = databaseSeedConfig,
        )
    }
}

// TODO: ADD COMMAND HANDLER!
data class UpdateBenchmarkCommand(
    val benchmarkId: UUID,
    val name: String,
    val description: String? = null,
    val benchmarkConfig: BenchmarkConfig,
    val systemsUnderTest: List<CreateBenchmarkCommand.UpdateSystemUnderTest>,
) : Command<UpdateBenchmarkCommand.Result> {

    @Schema(name = "UpdateBenchmarkResult", description = "Result of updating a benchmark")
    data class Result(
        @field:Schema(description = "ID of the updated benchmark")
        val benchmarkId: UUID,
    )
}

data class GenerateBehaviorModelsCommand(
    val benchmarkId: UUID,
) : Command<GenerateBehaviorModelsCommand.Result> {

    @Schema(name = "GenerateBehaviorModelsResult", description = "Result of generating behavior models")
    data class Result(
        @Schema(description = "ID of the updated load test configuration")
        val loadTestConfigId: UUID,
    )
}
