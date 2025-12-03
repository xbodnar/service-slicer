package cz.bodnor.serviceslicer.application.module.benchmark.command

import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateBenchmarkCommand(
    val name: String,
    val description: String? = null,
    val operationalSetting: OperationalSetting,
    val baselineSutId: UUID,
    val targetSutId: UUID,
) : Command<CreateBenchmarkCommand.Result> {

    @Schema(name = "CreateBenchmarkResult", description = "Result of creating a benchmark")
    data class Result(
        @field:Schema(description = "ID of the created benchmark")
        val benchmarkId: UUID,
    )
}

data class UpdateBenchmarkCommand(
    val benchmarkId: UUID,
    val name: String,
    val description: String? = null,
    val operationalSetting: OperationalSetting,
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
