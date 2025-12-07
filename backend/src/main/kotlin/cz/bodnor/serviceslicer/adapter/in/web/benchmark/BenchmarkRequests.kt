package cz.bodnor.serviceslicer.adapter.`in`.web.benchmark

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a benchmark")
data class CreateBenchmarkRequest(
    val name: String,
    val description: String? = null,
    val operationalSettingId: UUID,
    val baselineSutId: UUID,
    val targetSutId: UUID,
)

@Schema(description = "Request to update a benchmark")
data class UpdateBenchmarkRequest(
    val name: String,
    val description: String? = null,
)
