package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.query.GetOperationalSettingQuery
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetOperationalSettingQueryHandler(
    private val operationalSettingReadService: OperationalSettingReadService,
) : QueryHandler<OperationalSetting, GetOperationalSettingQuery> {
    override val query = GetOperationalSettingQuery::class

    override fun handle(query: GetOperationalSettingQuery): OperationalSetting =
        operationalSettingReadService.getById(query.operationalSettingId)
}
