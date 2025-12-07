package cz.bodnor.serviceslicer.application.module.operationalsetting.query

import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

class ListOperationalSettingsQuery(
    val page: Int = 0,
    val size: Int = 10,
) : Query<Page<OperationalSetting>> {
    fun toPageable() = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTimestamp"))
}

data class GetOperationalSettingQuery(
    val operationalSettingId: UUID,
) : Query<OperationalSetting>
