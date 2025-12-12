package cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting

import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

@Schema(description = "Request to create an operational setting")
data class CreateOperationalSettingRequest(
    val name: String,
    val description: String? = null,
    val openApiFileId: UUID,
    @Schema(description = "List of behavior models. Can be empty if generateUsageProfile is true")
    val usageProfile: List<BehaviorModel> = emptyList(),
    @Schema(
        description = "If true, behavior models will be auto-generated from the OpenAPI spec",
        defaultValue = "false",
    )
    val generateUsageProfile: Boolean = false,
    val operationalProfile: Map<Int, BigDecimal>,
)

data class UpdateOperationalSettingRequest(
    val name: String,
    val description: String? = null,
    val usageProfile: List<BehaviorModel>,
    val operationalProfile: Map<Int, BigDecimal>,
)
