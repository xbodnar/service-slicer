package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand.CreateUserBehaviorModelDto
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.UpdateLoadTestConfigCommand
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a load test experiment")
data class CreateLoadTestExperimentRequest(
    @Schema(description = "Name of the experiment", example = "Baseline Performance Test")
    val name: String,
    @Schema(
        description = "Description of the experiment",
        example = "Testing baseline performance with monolithic architecture",
    )
    val description: String? = null,
    @Schema(description = "Load test configuration")
    val loadTestConfig: CreateLoadTestConfig,
    @Schema(description = "List of systems under test to compare")
    val systemsUnderTest: List<CreateLoadTestExperimentCommand.CreateSystemUnderTest>,
) {

    @Schema(description = "Load test configuration")
    data class CreateLoadTestConfig(
        @Schema(description = "ID of the OpenAPI specification file")
        val openApiFileId: UUID,
        @Schema(description = "List of user behavior models")
        val behaviorModels: List<CreateUserBehaviorModelDto> = emptyList(),
        @Schema(description = "Operational profile defining load patterns")
        val operationalProfile: OperationalProfile? = null,
    ) {
        fun toCommand(): CreateLoadTestConfigCommand = CreateLoadTestConfigCommand(
            openApiFileId = openApiFileId,
            behaviorModels = behaviorModels,
            operationalProfile = operationalProfile,
        )
    }

    fun toCommand() = CreateLoadTestExperimentCommand(
        name = name,
        description = description,
        loadTestConfig = loadTestConfig.toCommand(),
        systemsUnderTest = systemsUnderTest,
    )
}

@Schema(description = "Request to update load test configuration")
data class UpdateLoadTestConfigRequest(
    @Schema(description = "ID of the OpenAPI specification file")
    val openApiFileId: UUID,
    @Schema(description = "List of user behavior models")
    val behaviorModels: List<CreateLoadTestConfigCommand.CreateUserBehaviorModelDto> = emptyList(),
    @Schema(description = "Operational profile defining load patterns")
    val operationalProfile: OperationalProfile? = null,
) {
    fun toCommand(experimentId: UUID) = UpdateLoadTestConfigCommand(
        experimentId = experimentId,
        openApiFileId = openApiFileId,
        behaviorModels = behaviorModels,
        operationalProfile = operationalProfile,
    )
}
