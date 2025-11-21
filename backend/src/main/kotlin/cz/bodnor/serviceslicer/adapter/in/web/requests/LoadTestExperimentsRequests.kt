package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand.CreateUserBehaviorModelDto
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.UpdateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalLoad
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a load test experiment")
data class CreateLoadTestExperimentRequest(
    @field:Schema(description = "Name of the experiment", example = "Baseline Performance Test")
    val name: String,
    @field:Schema(
        description = "Description of the experiment",
        example = "Testing baseline performance with monolithic architecture",
    )
    val description: String? = null,
    @field:Schema(description = "Load test configuration")
    val loadTestConfig: CreateLoadTestConfig,
    @field:Schema(description = "List of systems under test to compare")
    val systemsUnderTest: List<AddSystemUnderTestRequest>,
) {

    @Schema(description = "Load test configuration")
    data class CreateLoadTestConfig(
        @field:Schema(description = "ID of the OpenAPI specification file")
        val openApiFileId: UUID,
        @field:Schema(description = "List of user behavior models")
        val behaviorModels: List<CreateUserBehaviorModelDto>,
        @field:Schema(description = "Operational profile defining load patterns")
        val operationalProfile: List<OperationalLoad>,
        @field:Schema(description = "Whether to generate behavior models automatically")
        val generateBehaviorModels: Boolean = false,
    ) {
        init {
            require(behaviorModels.isNotEmpty() || generateBehaviorModels) {
                "Either behavior models must not be empty or generateBehaviorModels must be true"
            }
        }

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
    @field:Schema(description = "ID of the OpenAPI specification file")
    val openApiFileId: UUID,
    @field:Schema(description = "List of user behavior models")
    val behaviorModels: List<CreateLoadTestConfigCommand.CreateUserBehaviorModelDto> = emptyList(),
    @field:Schema(description = "Operational profile defining load patterns")
    val operationalProfile: List<OperationalLoad>,
) {
    fun toCommand(experimentId: UUID) = UpdateLoadTestConfigCommand(
        experimentId = experimentId,
        openApiFileId = openApiFileId,
        behaviorModels = behaviorModels,
        operationalProfile = operationalProfile,
    )
}
