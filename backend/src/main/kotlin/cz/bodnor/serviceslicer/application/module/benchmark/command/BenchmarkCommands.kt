package cz.bodnor.serviceslicer.application.module.benchmark.command

import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateBenchmarkCommand(
    val name: String,
    val description: String? = null,
    val operationalSettingId: UUID,
    val baselineSutId: UUID,
    val targetSutId: UUID,
) : Command<Benchmark>

data class UpdateBenchmarkCommand(
    val benchmarkId: UUID,
    val name: String,
    val description: String? = null,
) : Command<Benchmark>

data class GenerateBehaviorModelsCommand(
    val benchmarkId: UUID,
) : Command<GenerateBehaviorModelsCommand.Result> {

    @Schema(name = "GenerateBehaviorModelsResult", description = "Result of generating behavior models")
    data class Result(
        @Schema(description = "ID of the updated load test configuration")
        val loadTestConfigId: UUID,
    )
}
