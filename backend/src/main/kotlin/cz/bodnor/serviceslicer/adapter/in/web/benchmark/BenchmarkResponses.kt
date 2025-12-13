package cz.bodnor.serviceslicer.adapter.`in`.web.benchmark

import cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting.OperationalSettingDto
import cz.bodnor.serviceslicer.adapter.`in`.web.sut.DatabaseSeedConfigDto
import cz.bodnor.serviceslicer.adapter.`in`.web.sut.DockerConfigDto
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
    val systemsUnderTest: List<BenchmarkSystemUnderTestDto>,
)

data class BenchmarkSystemUnderTestDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
    val dockerConfig: DockerConfigDto,
    val databaseSeedConfigs: List<DatabaseSeedConfigDto>,
    val isBaseline: Boolean,
    val validationResult: ValidationResult?,
)
