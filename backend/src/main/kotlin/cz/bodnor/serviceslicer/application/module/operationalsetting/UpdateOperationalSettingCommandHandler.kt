package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.command.UpdateOperationalSettingCommand
import cz.bodnor.serviceslicer.application.module.operationalsetting.service.OpenApiParsingService
import cz.bodnor.serviceslicer.application.module.operationalsetting.service.ValidateOperationalSetting
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationReadService
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationWriteService
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingReadService
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class UpdateOperationalSettingCommandHandler(
    private val operationalSettingReadService: OperationalSettingReadService,
    private val operationalSettingWriteService: OperationalSettingWriteService,
    private val apiOperationReadService: ApiOperationReadService,
    private val apiOperationWriteService: ApiOperationWriteService,
    private val openApiParsingService: OpenApiParsingService,
) : CommandHandler<OperationalSetting, UpdateOperationalSettingCommand> {
    override val command = UpdateOperationalSettingCommand::class

    @Transactional
    override fun handle(command: UpdateOperationalSettingCommand): OperationalSetting {
        val operationalSetting = operationalSettingReadService.getById(command.operationalSettingId)

        operationalSetting.name = command.name
        operationalSetting.description = command.description
        operationalSetting.usageProfile = command.usageProfile
        operationalSetting.operationalProfile = command.operationalProfile

        val apiOperations = getApiOperations(operationalSetting.openApiFile.id)
        ValidateOperationalSetting(
            operationalSetting = operationalSetting,
            apiOperations = apiOperations,
        )

        return operationalSettingWriteService.save(operationalSetting)
    }

    private fun getApiOperations(openApiFileId: UUID): List<ApiOperation> {
        val apiOperations = apiOperationReadService.getByOpenApiFileId(openApiFileId)

        if (apiOperations.isNotEmpty()) {
            return apiOperations
        }

        val apiOperationsParsed = openApiParsingService.parse(openApiFileId)
        apiOperationWriteService.save(apiOperationsParsed)

        return apiOperationsParsed
    }
}
