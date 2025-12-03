package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.GenerateBehaviorModelsCommand
import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GenerateBehaviorModelsCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val generateBehaviorModels: GenerateBehaviorModels,
) : CommandHandler<GenerateBehaviorModelsCommand.Result, GenerateBehaviorModelsCommand> {

    override val command = GenerateBehaviorModelsCommand::class

    @Transactional
    override fun handle(command: GenerateBehaviorModelsCommand): GenerateBehaviorModelsCommand.Result {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        val loadTestConfig = benchmark.operationalSetting

        loadTestConfig.usageProfile = generateBehaviorModels(loadTestConfig.openApiFile.id)

        return GenerateBehaviorModelsCommand.Result(
            loadTestConfigId = loadTestConfig.id,
        )
    }
}
