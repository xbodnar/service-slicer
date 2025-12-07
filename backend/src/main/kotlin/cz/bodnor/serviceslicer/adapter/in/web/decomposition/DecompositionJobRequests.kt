package cz.bodnor.serviceslicer.adapter.`in`.web.decomposition

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a decomposition job")
data class CreateDecompositionJobRequest(
    val name: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val jarFileId: UUID,
)
