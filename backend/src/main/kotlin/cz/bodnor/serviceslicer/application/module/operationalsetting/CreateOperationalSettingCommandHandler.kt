package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.command.CreateOperationalSettingCommand
import cz.bodnor.serviceslicer.application.module.operationalsetting.service.OpenApiParsingService
import cz.bodnor.serviceslicer.application.module.operationalsetting.service.ValidateOperationalSetting
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateOperationalSettingCommandHandler(
    private val operationalSettingWriteService: OperationalSettingWriteService,
    private val fileReadService: FileReadService,
    private val openApiParsingService: OpenApiParsingService,
) : CommandHandler<CreateOperationalSettingCommand.Result, CreateOperationalSettingCommand> {
    override val command = CreateOperationalSettingCommand::class

    @Transactional
    override fun handle(command: CreateOperationalSettingCommand): CreateOperationalSettingCommand.Result {
        val file = fileReadService.getById(command.openApiFileId)
        verify(file.status == FileStatus.READY) { "File is not uploaded yet" }
        val apiOperations = openApiParsingService.parse(file.id)

        val operationalSetting = OperationalSetting(
            name = command.name,
            description = command.description,
            openApiFile = file,
            usageProfile = command.usageProfile,
            operationalProfile = command.operationalProfile,
        )

        ValidateOperationalSetting(
            operationalSetting = operationalSetting,
            apiOperations = apiOperations,
        )
        operationalSettingWriteService.save(operationalSetting)

        return CreateOperationalSettingCommand.Result(
            operationalSettingId = operationalSetting.id,
        )
    }
}
