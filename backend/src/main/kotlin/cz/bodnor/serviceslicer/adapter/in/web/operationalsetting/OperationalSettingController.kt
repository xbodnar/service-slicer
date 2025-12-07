package cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.command.DeleteOperationalSettingCommand
import cz.bodnor.serviceslicer.application.module.operationalsetting.query.GetOperationalSettingQuery
import cz.bodnor.serviceslicer.application.module.operationalsetting.query.ListOperationalSettingsQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/operational-settings")
class OperationalSettingController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    private val mapper: OperationalSettingMapper,
) {

    @GetMapping
    fun listOperationalSettings(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ListOperationalSettingsResponse = mapper.toDto(queryBus(ListOperationalSettingsQuery(page = page, size = size)))

    @GetMapping("/{operationalSettingId}")
    fun getOperationalSetting(@PathVariable operationalSettingId: UUID): OperationalSettingDto =
        mapper.toDto(queryBus(GetOperationalSettingQuery(operationalSettingId = operationalSettingId)))

    @PostMapping
    fun createOperationalSetting(@RequestBody request: CreateOperationalSettingRequest): OperationalSettingDto =
        mapper.toDto(commandBus(mapper.toCommand(request)))

    @DeleteMapping("/{operationalSettingId}")
    fun deleteOperationalSetting(@PathVariable operationalSettingId: UUID) =
        commandBus(DeleteOperationalSettingCommand(operationalSettingId = operationalSettingId))
}
