package cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileMapper
import cz.bodnor.serviceslicer.application.module.operationalsetting.command.CreateOperationalSettingCommand
import cz.bodnor.serviceslicer.application.module.operationalsetting.command.UpdateOperationalSettingCommand
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page
import java.util.UUID

@Mapper(componentModel = "spring", uses = [FileMapper::class])
interface OperationalSettingMapper {

    // INBOUND MAPPINGS
    fun toCommand(request: CreateOperationalSettingRequest): CreateOperationalSettingCommand

    @Mapping(target = "operationalSettingId", source = "operationalSettingId")
    fun toCommand(
        request: UpdateOperationalSettingRequest,
        operationalSettingId: UUID,
    ): UpdateOperationalSettingCommand

    // OUTBOUND MAPPINGS
    fun toDto(result: OperationalSetting): OperationalSettingDto

    @Mapping(target = "items", source = "content", defaultExpression = "java(java.util.List.of())")
    @Mapping(target = "currentPage", source = "number")
    @Mapping(target = "pageSize", source = "size")
    fun toDto(result: Page<OperationalSetting>): ListOperationalSettingsResponse
}
