package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateOperationalSettingRequest
import cz.bodnor.serviceslicer.application.module.operationalsetting.command.CreateOperationalSettingCommand
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/operational-settings")
class OperationalSettingController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {

    @GetMapping
    fun listOperationalSettings(): ListOperationalSettingsQuery.Result = queryBus(ListOperationalSettingsQuery())

    @GetMapping("/{operationalSettingId}")
    fun getOperationalSetting(@PathVariable operationalSettingId: UUID) =
        queryBus(GetOperationalSettingQuery(operationalSettingId = operationalSettingId))

    @PostMapping
    fun createOperationalSetting(
        @RequestBody request: CreateOperationalSettingRequest,
    ): CreateOperationalSettingCommand.Result = commandBus(request.toCommand())

    @DeleteMapping("/{operationalSettingId}")
    fun deleteOperationalSetting(@PathVariable operationalSettingId: UUID) =
        commandBus(DeleteOperationalSettingCommand(operationalSettingId = operationalSettingId))
}
