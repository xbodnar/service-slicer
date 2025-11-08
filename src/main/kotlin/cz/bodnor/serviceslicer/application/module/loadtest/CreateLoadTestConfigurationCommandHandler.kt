package cz.bodnor.serviceslicer.application.module.loadtest

import cz.bodnor.serviceslicer.application.module.loadtest.command.CreateLoadTestConfigurationCommand
import cz.bodnor.serviceslicer.application.module.loadtest.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.application.module.loadtest.port.out.SaveApiOperations
import cz.bodnor.serviceslicer.application.module.loadtest.service.OpenApiParsingService
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationWriteService
import cz.bodnor.serviceslicer.domain.loadtest.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtest.LoadTestConfigWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateLoadTestConfigurationCommandHandler(
    private val loadTestConfigWriteService: LoadTestConfigWriteService,
    private val openApiParsingService: OpenApiParsingService,
    private val generateBehaviorModels: GenerateBehaviorModels,
    private val saveApiOperations: SaveApiOperations,
) : CommandHandler<CreateLoadTestConfigurationCommand.Result, CreateLoadTestConfigurationCommand> {

    override val command = CreateLoadTestConfigurationCommand::class

    @Transactional
    override fun handle(command: CreateLoadTestConfigurationCommand): CreateLoadTestConfigurationCommand.Result {
        validateInput(command)

        // Parse OpenAPI file and persist ApiOperations
        val apiOperations = openApiParsingService.parse(command.openApiFileId)
        saveApiOperations(apiOperations)

        val operationToEntityMap = apiOperations.associateBy { it.name }

        // Validate that all operation IDs in behavior models exist
        require(command.behaviorModels.flatMap { it.steps }.all { operationToEntityMap.containsKey(it) }) {
            "Unknown operation ID in behavior model steps"
        }

        val behaviorModels = if (command.behaviorModels.isNotEmpty()) {
            command.behaviorModels.mapIndexed { index, model ->
                BehaviorModel(
                    id = model.id,
                    actor = model.actor,
                    usageProfile = model.behaviorProbability,
                    steps = model.steps,
                    thinkFrom = model.thinkFrom,
                    thinkTo = model.thinkTo,
                )
            }
        } else {
            generateBehaviorModels(command.openApiFileId)
        }

        val loadTestConfig = loadTestConfigWriteService.create(
            openApiFileId = command.openApiFileId,
            name = command.name,
            behaviorModels = behaviorModels,
            operationalProfile = command.operationalProfile,
            k6Configuration = null,
        )

        return CreateLoadTestConfigurationCommand.Result(loadTestConfig)
    }

    private fun validateInput(command: CreateLoadTestConfigurationCommand) {
        if (command.behaviorModels.isNotEmpty()) {
            require(command.behaviorModels.sumOf { it.behaviorProbability } == 1.0) {
                "Sum of behavior probabilities must be 1.0"
            }
        }
        command.operationalProfile?.let {
            require(command.operationalProfile.freq.sumOf { it } == 1.0) {
                "Sum of load probabilities must be 1.0"
            }
        }
    }
}
