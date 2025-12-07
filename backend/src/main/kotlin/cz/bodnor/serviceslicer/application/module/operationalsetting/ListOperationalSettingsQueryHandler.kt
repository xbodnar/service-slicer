package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.query.ListOperationalSettingsQuery
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class ListOperationalSettingsQueryHandler(
    private val operationalSettingReadService: OperationalSettingReadService,
) : QueryHandler<Page<OperationalSetting>, ListOperationalSettingsQuery> {

    override val query = ListOperationalSettingsQuery::class

    override fun handle(query: ListOperationalSettingsQuery): Page<OperationalSetting> =
        operationalSettingReadService.findAll(query.toPageable())
}
