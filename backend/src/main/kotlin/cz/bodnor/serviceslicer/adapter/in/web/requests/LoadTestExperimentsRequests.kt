package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.UpdateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import java.util.UUID

data class CreateLoadTestExperimentRequest(
    val name: String,
    val description: String? = null,
    val loadTestConfig: CreateLoadTestExperimentCommand.CreateLoadTestConfigDto,
    val systemsUnderTest: List<CreateLoadTestExperimentCommand.CreateSystemUnderTestDto>,
) {
    fun toCommand() = CreateLoadTestExperimentCommand(
        name = name,
        description = description,
        loadTestConfig = loadTestConfig,
        systemsUnderTest = systemsUnderTest,
    )
}

data class AddSystemUnderTestRequest(
    val name: String,
    val composeFileId: UUID,
    val jarFileId: UUID,
    val description: String? = null,
    val healthCheckPath: String = "/actuator/health",
    val appPort: Int = 9090,
    val startupTimeoutSeconds: Long = 180,
) {
    fun toCommand(experimentId: UUID) = AddSystemUnderTestCommand(
        experimentId = experimentId,
        name = name,
        composeFileId = composeFileId,
        jarFileId = jarFileId,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}

data class UpdateLoadTestConfigRequest(
    val openApiFileId: UUID,
    val behaviorModels: List<CreateLoadTestConfigCommand.CreateUserBehaviorModelDto> = emptyList(),
    val operationalProfile: OperationalProfile? = null,
) {
    fun toCommand(experimentId: UUID) = UpdateLoadTestConfigCommand(
        experimentId = experimentId,
        openApiFileId = openApiFileId,
        behaviorModels = behaviorModels,
        operationalProfile = operationalProfile,
    )
}

data class UpdateSystemUnderTestRequest(
    val name: String,
    val composeFileId: UUID,
    val jarFileId: UUID,
    val description: String? = null,
    val healthCheckPath: String = "/actuator/health",
    val appPort: Int = 9090,
    val startupTimeoutSeconds: Long = 180,
) {
    fun toCommand(
        experimentId: UUID,
        sutId: UUID,
    ) = UpdateSystemUnderTestCommand(
        experimentId = experimentId,
        sutId = sutId,
        name = name,
        composeFileId = composeFileId,
        jarFileId = jarFileId,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}
