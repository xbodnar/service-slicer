package cz.bodnor.serviceslicer.application.module.loadtestconfig

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.GenerateBehaviorModels
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.SaveApiOperations
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.OpenApiParsingService
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.ValidateLoadTestConfig
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfig
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
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
        verify(file.status == FileStatus.READY) { "File is not uploaded yet" }

        val loadTestConfig = LoadTestConfig(
            openApiFileId = command.openApiFileId,
            behaviorModels = command.behaviorModels.mapIndexed { index, model ->
                BehaviorModel(
                    id = model.id,
                    actor = model.actor,
                    usageProfile = model.usageProfile,
                    steps = model.steps,
                    thinkFrom = model.thinkFrom,
                    thinkTo = model.thinkTo,
                )
            },
            operationalProfile = command.operationalProfile,
            k6Configuration = null,
        )

        val apiOperations = openApiParsingService.parse(command.openApiFileId)

        ValidateLoadTestConfig(loadTestConfig, apiOperations)
        saveApiOperations(apiOperations)
        loadTestConfigWriteService.create(loadTestConfig)

        return CreateLoadTestConfigCommand.Result(loadTestConfig.id)
    }
}
