package cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileDto
import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "List of operational settings")
data class ListOperationalSettingsResponse(
    val items: List<OperationalSettingDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

@Schema(description = "Operational setting")
data class OperationalSettingDto(
    val id: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
    val openApiFile: FileDto,
    val usageProfile: List<BehaviorModel>,
    val operationalProfile: Map<Int, BigDecimal>,
)
