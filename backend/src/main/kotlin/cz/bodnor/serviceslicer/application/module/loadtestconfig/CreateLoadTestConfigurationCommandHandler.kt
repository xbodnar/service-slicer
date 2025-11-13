package cz.bodnor.serviceslicer.application.module.loadtestconfig

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.SaveApiOperations
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.OpenApiParsingService
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.ValidateLoadTestConfig
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
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
    private val fileReadService: FileReadService,
) : CommandHandler<CreateLoadTestConfigCommand.Result, CreateLoadTestConfigCommand> {

    override val command = CreateLoadTestConfigCommand::class

    @Transactional
    override fun handle(command: CreateLoadTestConfigCommand): CreateLoadTestConfigCommand.Result {
        // Parse OpenAPI file and persist ApiOperations
        val file = fileReadService.getById(command.openApiFileId)
        require(file.status == FileStatus.READY) { "File is not uploaded yet" }

        val apiOperations = openApiParsingService.parse(command.openApiFileId)
        val behaviorModels = command.behaviorModels.mapIndexed { index, model ->
            BehaviorModel(
                id = model.id,
                actor = model.actor,
                usageProfile = model.usageProfile,
                steps = model.steps,
                thinkFrom = model.thinkFrom,
                thinkTo = model.thinkTo,
            )
        }

        ValidateLoadTestConfig(behaviorModels, apiOperations, command.operationalProfile)
        saveApiOperations(apiOperations)

        val loadTestConfig = loadTestConfigWriteService.create(
            openApiFileId = command.openApiFileId,
            behaviorModels = behaviorModels,
            operationalProfile = command.operationalProfile,
            k6Configuration = null,
        )

        return CreateLoadTestConfigCommand.Result(loadTestConfig.id)
    }
}
