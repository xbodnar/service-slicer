package cz.bodnor.serviceslicer.application.module.loadtestexperiment.command

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand.CreateUserBehaviorModelDto
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateLoadTestExperimentCommand(
    val name: String,
    val description: String? = null,
    val loadTestConfig: CreateLoadTestConfigCommand,
    val systemsUnderTest: List<CreateSystemUnderTest>,
) : Command<CreateLoadTestExperimentCommand.Result> {

    data class CreateSystemUnderTest(
        val name: String,
        val composeFileId: UUID,
        val jarFileId: UUID,
        val description: String? = null,
        val healthCheckPath: String = "/actuator/health",
        val appPort: Int = 9090,
        val startupTimeoutSeconds: Long = 180,
    )

    data class Result(
        val experimentId: UUID,
    )
}

data class UpdateLoadTestConfigCommand(
    val experimentId: UUID,
    val openApiFileId: UUID,
    val behaviorModels: List<CreateUserBehaviorModelDto> = emptyList(),
    val operationalProfile: OperationalProfile? = null,
) : Command<UpdateLoadTestConfigCommand.Result> {

    data class Result(
        val loadTestConfigId: UUID,
    )
}

data class GenerateBehaviorModelsCommand(
    val experimentId: UUID,
) : Command<GenerateBehaviorModelsCommand.Result> {

    data class Result(
        val loadTestConfigId: UUID,
    )
}
