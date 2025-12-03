package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.query.ListOperationalSettingsQuery
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class ListOperationalSettingsQueryHandler(
    private val operationalSettingReadService: OperationalSettingReadService,
) : QueryHandler<ListOperationalSettingsQuery.Result, ListOperationalSettingsQuery> {

    override val query = ListOperationalSettingsQuery::class

    override fun handle(query: ListOperationalSettingsQuery): ListOperationalSettingsQuery.Result =
        ListOperationalSettingsQuery.Result(
            operationalSettings = operationalSettingReadService.findAll(),
        )
}
