package cz.bodnor.serviceslicer.application.module.loadtestconfig.command

import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateLoadTestConfigCommand(
    val openApiFileId: UUID,
    val behaviorModels: List<CreateUserBehaviorModelDto> = emptyList(),
    val operationalProfile: OperationalProfile? = null,
) : Command<CreateLoadTestConfigCommand.Result> {

    data class CreateUserBehaviorModelDto(
        val id: String,
        // Name of the actor persona this behavior model represents
        val actor: String,
        // Probability of this behavior model being executed, must be between 0 and 1
        val usageProfile: Double,
        // Sequence of operations that the actor performs, IDs of the operations in the OpenAPI file
        val steps: List<String>,
        // Think time range (in milliseconds)
        val thinkFrom: Int,
        val thinkTo: Int,
    )

    data class Result(
        val loadTestConfigId: UUID,
    )
}
