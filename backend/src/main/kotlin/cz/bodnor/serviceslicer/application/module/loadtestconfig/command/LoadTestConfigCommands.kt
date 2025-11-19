package cz.bodnor.serviceslicer.application.module.loadtestconfig.command

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand.CreateUserBehaviorModelDto
import cz.bodnor.serviceslicer.domain.loadtestconfig.ApiRequest
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalLoad
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateLoadTestConfigCommand(
    val openApiFileId: UUID,
    val behaviorModels: List<CreateUserBehaviorModelDto>,
    val operationalProfile: List<OperationalLoad>,
) : Command<CreateLoadTestConfigCommand.Result> {

    @Schema(description = "User behavior model configuration")
    data class CreateUserBehaviorModelDto(
        @Schema(description = "Unique identifier for the behavior model", example = "bm1")
        val id: String,
        @Schema(description = "Name of the actor persona", example = "Customer")
        val actor: String,
        @Schema(description = "Probability of this behavior model being executed (0-1)", example = "0.5")
        val usageProfile: Double,
        @Schema(description = "Sequence of operation IDs from OpenAPI file")
        val steps: List<ApiRequest>,
        @Schema(description = "Minimum think time in milliseconds", example = "1000")
        val thinkFrom: Int,
        @Schema(description = "Maximum think time in milliseconds", example = "3000")
        val thinkTo: Int,
    )

    @Schema(name = "CreateLoadTestConfigResult", description = "Result of creating load test configuration")
    data class Result(
        @Schema(description = "ID of the created load test configuration")
        val loadTestConfigId: UUID,
    )
}

data class UpdateLoadTestConfigCommand(
    val experimentId: UUID,
    val openApiFileId: UUID,
    val behaviorModels: List<CreateUserBehaviorModelDto>,
    val operationalProfile: List<OperationalLoad>,
) : Command<UpdateLoadTestConfigCommand.Result> {

    @Schema(name = "UpdateLoadTestConfigResult", description = "Result of updating load test configuration")
    data class Result(
        @Schema(description = "ID of the updated load test configuration")
        val loadTestConfigId: UUID,
    )
}

data class GenerateBehaviorModelsCommand(
    val experimentId: UUID,
) : Command<GenerateBehaviorModelsCommand.Result> {

    @Schema(name = "GenerateBehaviorModelsResult", description = "Result of generating behavior models")
    data class Result(
        @Schema(description = "ID of the updated load test configuration")
        val loadTestConfigId: UUID,
    )
}
