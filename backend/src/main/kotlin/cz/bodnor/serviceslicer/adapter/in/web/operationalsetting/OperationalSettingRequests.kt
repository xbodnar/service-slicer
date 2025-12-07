package cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting

import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import java.math.BigDecimal
import java.util.UUID

data class CreateOperationalSettingRequest(
    val name: String,
    val description: String? = null,
    val openApiFileId: UUID,
    val usageProfile: List<BehaviorModel>,
    val operationalProfile: Map<Int, BigDecimal>,
)
