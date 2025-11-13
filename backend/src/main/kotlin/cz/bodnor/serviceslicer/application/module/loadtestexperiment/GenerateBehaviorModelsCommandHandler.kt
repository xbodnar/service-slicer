package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.GenerateBehaviorModelsCommand
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GenerateBehaviorModelsCommandHandler(
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
    private val loadTestConfigReadService: LoadTestConfigReadService,
    private val generateBehaviorModels: GenerateBehaviorModels,
) : CommandHandler<GenerateBehaviorModelsCommand.Result, GenerateBehaviorModelsCommand> {

    override val command = GenerateBehaviorModelsCommand::class

    @Transactional
    override fun handle(command: GenerateBehaviorModelsCommand): GenerateBehaviorModelsCommand.Result {
        val loadTestExperiment = loadTestExperimentReadService.getById(command.experimentId)
        val loadTestConfig = loadTestConfigReadService.getById(loadTestExperiment.loadTestConfigId)

        val behaviorModels = generateBehaviorModels(loadTestConfig.openApiFileId)
        loadTestConfig.behaviorModels = behaviorModels

        return GenerateBehaviorModelsCommand.Result(
            loadTestConfigId = loadTestConfig.id,
        )
    }
}
