package cz.bodnor.serviceslicer.adapter.`in`.web.decomposition

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileDto
import cz.bodnor.serviceslicer.domain.decompositioncandidate.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionMethod
import cz.bodnor.serviceslicer.domain.job.JobStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

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
    val dependencyGraph: List<ClassNodeDto>,
    val decompositionCandidates: List<DecompositionCandidateDto>,
)

@Schema(description = "Decomposition job")
data class DecompositionJobDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val monolithArtifact: MonolithArtifactDto,
    val status: JobStatus,
    val startTimestamp: Instant?,
    val endTimestamp: Instant?,
)

data class MonolithArtifactDto(
    val id: UUID,
    val createdAt: Instant,
    val basePackageName: String,
    val excludePackages: List<String>,
    val jarFile: FileDto,
)

data class DecompositionCandidateDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val method: DecompositionMethod,
    val modularity: BigDecimal?,
    val serviceBoundaries: List<ServiceBoundaryDto>,
)

data class ServiceBoundaryDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val metrics: BoundaryMetrics,
    val typeNames: List<String>,
)

data class ClassNodeDto(
    val id: UUID,
    val simpleClassName: String,
    val fullyQualifiedClassName: String,
    val dependencies: List<String>,
)
