package cz.bodnor.serviceslicer.application.module.benchmarkrun.query

import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunState
import cz.bodnor.serviceslicer.domain.benchmarkrun.OperationId
import cz.bodnor.serviceslicer.domain.benchmarkrun.OperationMetrics
import cz.bodnor.serviceslicer.domain.benchmarkrun.TestCaseStatus
import cz.bodnor.serviceslicer.domain.benchmarkrun.TestSuiteStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ListBenchmarkRunsQuery(val benchmarkId: UUID) : Query<ListBenchmarkRunsQuery.Result> {

    @Schema(name = "ListBenchmarkRunsResult", description = "List of benchmark runs")
    data class Result(
        @Schema(description = "List of benchmark run summaries")
        val benchmarkRuns: List<BenchmarkRunSummary>,
    )

    @Schema(description = "Summary of a benchmark run")
    data class BenchmarkRunSummary(
        @Schema(description = "ID of the benchmark run")
        val benchmarkRunId: UUID,
        @Schema(description = "ID of the benchmark")
        val benchmarkId: UUID,
        @Schema(description = "State of the benchmark run")
        val state: BenchmarkRunState,
        @Schema(description = "Number of systems under test in this run")
        val sutCount: Int,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: Instant,
    )
}

data class GetBenchmarkRunQuery(val benchmarkId: UUID, val benchmarkRunId: UUID) : Query<GetBenchmarkRunQuery.Result> {

    @Schema(name = "GetBenchmarkRunResult", description = "Detailed benchmark run information")
    data class Result(
        @Schema(description = "ID of the benchmark run")
        val benchmarkRunId: UUID,
        @Schema(description = "ID of the benchmark")
        val benchmarkId: UUID,
        @Schema(description = "State of the benchmark run")
        val state: BenchmarkRunState,
        @Schema(description = "List of SUT load test runs")
        val architectureTestSuites: List<ArchitectureTestSuiteDto>,
        @Schema(description = "Baseline test case")
        val baselineTestCase: BaselineTestCaseDto?,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: Instant,
    )

    @Schema(description = "Baseline test case DTO")
    data class BaselineTestCaseDto(
        @Schema(description = "ID of the baseline test case")
        val id: UUID,
        @Schema(description = "ID of the baseline SUT")
        val baselineSutId: UUID,
        @Schema(description = "Load level")
        val load: Int,
        @Schema(description = "Status of the test case")
        val status: TestCaseStatus,
        @Schema(description = "Start timestamp")
        val startTimestamp: Instant,
        @Schema(description = "End timestamp")
        val endTimestamp: Instant?,
        @Schema(description = "Operation metrics by operation ID")
        val operationMeasurements: Map<OperationId, OperationMetrics>,
        @Schema(description = "Scalability thresholds by operation ID")
        val scalabilityThresholds: Map<OperationId, BigDecimal>,
        @Schema(description = "K6 output")
        val k6Output: String?,
    )

    @Schema(description = "Architecture test suite DTO")
    data class ArchitectureTestSuiteDto(
        @Schema(description = "ID of the test suite")
        val id: UUID,
        @Schema(description = "ID of the target SUT")
        val targetSutId: UUID,
        @Schema(description = "Status of the test suite")
        val status: TestSuiteStatus,
        @Schema(description = "List of target test cases")
        val targetTestCases: List<TargetTestCaseDto>,
        @Schema(description = "Scalability footprint by operation ID")
        val scalabilityFootprint: Map<OperationId, Int>,
    )

    @Schema(description = "Target test case DTO")
    data class TargetTestCaseDto(
        @Schema(description = "ID of the test case")
        val id: UUID,
        @Schema(description = "Load level")
        val load: Int,
        @Schema(description = "Load frequency")
        val loadFrequency: Double,
        @Schema(description = "Status of the test case")
        val status: TestCaseStatus,
        @Schema(description = "Start timestamp")
        val startTimestamp: Instant?,
        @Schema(description = "End timestamp")
        val endTimestamp: Instant?,
        @Schema(description = "Operation metrics by operation ID")
        val operationMeasurements: Map<OperationId, OperationMetrics>,
        @Schema(description = "Pass scalability threshold by operation ID")
        val passScalabilityThreshold: Map<OperationId, Boolean>,
        @Schema(description = "Scalability shares by operation ID")
        val scalabilityShares: Map<OperationId, BigDecimal>,
        @Schema(description = "Relative domain metric")
        val relativeDomainMetric: BigDecimal?,
        @Schema(description = "K6 output")
        val k6Output: String?,
    )
}
