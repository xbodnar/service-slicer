package cz.bodnor.serviceslicer.adapter.`in`.web.decomposition

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileDto
import cz.bodnor.serviceslicer.application.module.decomposition.query.GetDecompositionJobSummaryQuery
import cz.bodnor.serviceslicer.domain.job.JobStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "List of decomposition jobs")
data class ListDecompositionJobsResponse(
    val items: List<DecompositionJobDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

@Schema(description = "Decomposition job summary")
data class DecompositionJobSummaryDto(
    val decompositionJob: DecompositionJobDto,
    val dependencyGraph: GetDecompositionJobSummaryQuery.GraphSummary,
    val decompositionResults: GetDecompositionJobSummaryQuery.DecompositionResults,
)

@Schema(description = "Decomposition job")
data class DecompositionJobDto(
    val id: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val monolithArtifact: MonolithArtifactDto,
    val status: JobStatus,
    val startTimestamp: Instant?,
    val endTimestamp: Instant?,
)

data class MonolithArtifactDto(
    val id: String,
    val createdAt: Instant,
    val basePackageName: String,
    val excludePackages: List<String>,
    val jarFile: FileDto,
)
