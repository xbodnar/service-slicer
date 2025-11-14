package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.file.port.out.DeleteFileFromStorage
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.SaveApiOperations
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.OpenApiParsingService
import cz.bodnor.serviceslicer.application.module.loadtestconfig.service.ValidateLoadTestConfig
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.UpdateLoadTestConfigCommand
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationReadService
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationWriteService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateLoadTestConfigCommandHandler(
    private val loadTestConfigReadService: LoadTestConfigReadService,
    private val loadTestExperimentWriteService: LoadTestExperimentWriteService,
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
    private val openApiParsingService: OpenApiParsingService,
    private val saveApiOperations: SaveApiOperations,
    private val fileReadService: FileReadService,
    private val deleteFileFromStorage: DeleteFileFromStorage,
    private val apiOperationReadService: ApiOperationReadService,
    private val apiOperationWriteService: ApiOperationWriteService,
) : CommandHandler<UpdateLoadTestConfigCommand.Result, UpdateLoadTestConfigCommand> {

    override val command = UpdateLoadTestConfigCommand::class

    @Transactional
    override fun handle(command: UpdateLoadTestConfigCommand): UpdateLoadTestConfigCommand.Result {
        val loadTestExperiment = loadTestExperimentReadService.getById(command.experimentId)
        val loadTestConfig = loadTestConfigReadService.getById(loadTestExperiment.loadTestConfigId)

        val apiOperations = if (loadTestConfig.openApiFileId != command.openApiFileId) {
            val newFile = fileReadService.getById(command.openApiFileId)
            require(newFile.status == FileStatus.READY) { "File is not uploaded yet" }

            val apiOperations = openApiParsingService.parse(command.openApiFileId)
            saveApiOperations(apiOperations)
            loadTestConfig.openApiFileId = newFile.id

            val oldFile = fileReadService.getById(loadTestConfig.openApiFileId)
            deleteFileFromStorage(oldFile.storageKey)
            apiOperationWriteService.deleteByOpenApiFileId(oldFile.id)

            apiOperations
        } else {
            apiOperationReadService.getByOpenApiFileId(loadTestConfig.openApiFileId)
        }

        val behaviorModels = command.behaviorModels.map { model ->
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
        loadTestConfig.behaviorModels = behaviorModels
        loadTestConfig.operationalProfile = command.operationalProfile

        return UpdateLoadTestConfigCommand.Result(
            loadTestConfigId = loadTestConfig.id,
        )
    }
}
