package cz.bodnor.serviceslicer.application.module.operationalsetting.query

import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.util.UUID

class ListOperationalSettingsQuery : Query<ListOperationalSettingsQuery.Result> {

    data class Result(
        val operationalSettings: List<OperationalSetting>,
    )
}

data class GetOperationalSettingQuery(
    val operationalSettingId: UUID,
) : Query<OperationalSetting>
