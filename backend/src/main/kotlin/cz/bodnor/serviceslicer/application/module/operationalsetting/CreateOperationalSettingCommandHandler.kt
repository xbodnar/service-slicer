package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GenerateUsageProfile
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
    private val generateUsageProfile: GenerateUsageProfile,
) : CommandHandler<OperationalSetting, CreateOperationalSettingCommand> {
    override val command = CreateOperationalSettingCommand::class

    @Transactional
    override fun handle(command: CreateOperationalSettingCommand): OperationalSetting {
        val file = fileReadService.getById(command.openApiFileId)
        verify(file.status == FileStatus.READY) { "File is not uploaded yet" }
        verify(command.usageProfile.isNotEmpty() || command.generateUsageProfile) {
            "Usage profile is empty and automatic generation is disabled"
        }
        val apiOperations = openApiParsingService.parse(file.id)

        val usageProfile = if (command.generateUsageProfile) {
            generateUsageProfile(file.id)
        } else {
            command.usageProfile
        }

        val operationalSetting = OperationalSetting(
            name = command.name,
            description = command.description,
            openApiFile = file,
            usageProfile = usageProfile,
            operationalProfile = command.operationalProfile,
        )

        ValidateOperationalSetting(
            operationalSetting = operationalSetting,
            apiOperations = apiOperations,
        )

        return operationalSettingWriteService.save(operationalSetting)
    }
}
