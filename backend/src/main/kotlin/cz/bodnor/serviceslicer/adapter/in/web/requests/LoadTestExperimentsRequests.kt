package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand.CreateUserBehaviorModelDto
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.UpdateLoadTestConfigCommand
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import java.util.UUID

data class CreateLoadTestExperimentRequest(
    val name: String,
    val description: String? = null,
    val loadTestConfig: CreateLoadTestConfig,
    val systemsUnderTest: List<CreateLoadTestExperimentCommand.CreateSystemUnderTest>,
) {

    data class CreateLoadTestConfig(
        val openApiFileId: UUID,
        val behaviorModels: List<CreateUserBehaviorModelDto> = emptyList(),
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
