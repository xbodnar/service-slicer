package cz.bodnor.serviceslicer.application.module.loadtestconfig

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.SaveApiOperations
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.OpenApiParsingService
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateLoadTestConfigurationCommandHandler(
    private val loadTestConfigWriteService: LoadTestConfigWriteService,
    private val openApiParsingService: OpenApiParsingService,
    private val generateBehaviorModels: GenerateBehaviorModels,
    private val saveApiOperations: SaveApiOperations,
) : CommandHandler<CreateLoadTestConfigCommand.Result, CreateLoadTestConfigCommand> {

    override val command = CreateLoadTestConfigCommand::class

    @Transactional
    override fun handle(command: CreateLoadTestConfigCommand): CreateLoadTestConfigCommand.Result {
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
            behaviorModels = behaviorModels,
            operationalProfile = command.operationalProfile,
            k6Configuration = null,
        )

        return CreateLoadTestConfigCommand.Result(loadTestConfig.id)
    }

    private fun validateInput(command: CreateLoadTestConfigCommand) {
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
