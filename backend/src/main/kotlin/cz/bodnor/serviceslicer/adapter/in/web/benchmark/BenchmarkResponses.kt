package cz.bodnor.serviceslicer.adapter.`in`.web.benchmark

import cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting.OperationalSettingDto
import cz.bodnor.serviceslicer.adapter.`in`.web.sut.SystemUnderTestDetailDto
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "List of benchmarks")
data class ListBenchmarksResponse(
    val items: List<BenchmarkDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

data class BenchmarkDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
)

data class BenchmarkDetailDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
    val operationalSetting: OperationalSettingDto,
    val baselineSut: SystemUnderTestDetailDto,
    val targetSut: SystemUnderTestDetailDto,
    val baselineSutValidationResult: ValidationResult?,
    val targetSutValidationResult: ValidationResult?,
)

data class BenchmarkRunDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,

)
