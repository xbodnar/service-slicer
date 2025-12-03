package cz.bodnor.serviceslicer.application.module.benchmark.query

import cz.bodnor.serviceslicer.application.module.sut.query.SystemUnderTestDto
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ListBenchmarksQuery(val dummy: Unit = Unit) : Query<ListBenchmarksQuery.Result> {

    @Schema(name = "ListBenchmarksResult", description = "List of benchmarks")
    data class Result(
        @Schema(description = "List of benchmark summaries")
        val benchmarks: List<BenchmarkSummary>,
    )

    @Schema(description = "Summary of a benchmark")
    data class BenchmarkSummary(
        @Schema(description = "ID of the benchmark")
        val benchmarkId: UUID,
        @Schema(description = "Name of the benchmark")
        val name: String,
        @Schema(description = "Description of the benchmark")
        val description: String?,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
    )
}

data class GetBenchmarkQuery(val benchmarkId: UUID) : Query<GetBenchmarkQuery.Result> {

    @Schema(name = "GetBenchmarkResult", description = "Detailed benchmark information")
    data class Result(
        @Schema(description = "ID of the benchmark")
        val id: UUID,
        @Schema(description = "Name of the benchmark")
        val name: String,
        @Schema(description = "Description of the benchmark")
        val description: String?,
        @Schema(description = "Load test configuration")
        val operationalSetting: OperationalSetting,
        @Schema(description = "Baseline system under test")
        val baselineSut: SystemUnderTestDto,
        @Schema(description = "Target system under test")
        val targetSut: SystemUnderTestDto,
        @Schema(description = "Validation result for baseline SUT")
        val baselineSutValidationResult: ValidationResult?,
        @Schema(description = "Validation result for target SUT")
        val targetSutValidationResult: ValidationResult?,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: Instant,
    )
}
