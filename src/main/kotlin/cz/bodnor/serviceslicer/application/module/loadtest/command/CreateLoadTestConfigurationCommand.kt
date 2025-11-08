package cz.bodnor.serviceslicer.application.module.loadtest.command

import cz.bodnor.serviceslicer.domain.loadtest.LoadTestConfig
import cz.bodnor.serviceslicer.domain.loadtest.OperationalProfile
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateLoadTestConfigurationCommand(
    val openApiFileId: UUID,
    val name: String,
    val behaviorModels: List<UserBehaviorModel> = emptyList(),
    val operationalProfile: OperationalProfile? = null,
) : Command<CreateLoadTestConfigurationCommand.Result> {

    data class UserBehaviorModel(
        val id: String,
        // Name of the actor persona this behavior model represents
        val actor: String,
        // Probability of this behavior model being executed, must be between 0 and 1
        val behaviorProbability: Double,
        // Sequence of operations that the actor performs, IDs of the operations in the OpenAPI file
        val steps: List<String>,
        // Think time range (in milliseconds)
        val thinkFrom: Int,
        val thinkTo: Int,
    )

    data class Result(
        val loadTestConfig: LoadTestConfig,
    )
}
