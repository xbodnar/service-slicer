package cz.bodnor.serviceslicer.application.module.benchmarkrun.query

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunState
import cz.bodnor.serviceslicer.domain.benchmarkrun.ExperimentResults
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCaseOperationMetrics
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import cz.bodnor.serviceslicer.domain.testcase.TargetTestCaseOperationMetrics
import cz.bodnor.serviceslicer.domain.testcase.TestCaseStatus
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
        val id: UUID,
        @Schema(description = "ID of the benchmark")
        val benchmarkId: UUID,
        @Schema(description = "Baseline test case")
        val baselineTestCase: BaselineTestCaseDto,
        @Schema(description = "List of target test cases")
        val targetTestCases: List<TargetTestCaseDto>,
        @Schema(description = "Test duration")
        val testDuration: String,
        @Schema(description = "State of the benchmark run")
        val state: BenchmarkRunState,
        @Schema(description = "Experiment results")
        val experimentResults: ExperimentResults?,
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
        @Schema(description = "Relative domain metrics by load level")
        val relativeDomainMetrics: Map<Int, BigDecimal>,
        @Schema(description = "Load level")
        val load: Int,
        @Schema(description = "Status of the test case")
        val status: TestCaseStatus,
        @Schema(description = "Start timestamp")
        val startTimestamp: Instant?,
        @Schema(description = "End timestamp")
        val endTimestamp: Instant?,
        @Schema(description = "Operation metrics by operation ID")
        val operationMetrics: Map<OperationId, BaselineTestCaseOperationMetrics>,
        @Schema(description = "K6 output")
        val k6Output: String?,
        @Schema(description = "JSON summary of the K6 run")
        val jsonSummary: JsonNode?,
    )

    @Schema(description = "Target test case DTO")
    data class TargetTestCaseDto(
        @Schema(description = "ID of the test case")
        val id: UUID,
        @Schema(description = "Load level")
        val load: Int,
        @Schema(description = "Load frequency")
        val loadFrequency: BigDecimal,
        @Schema(description = "Status of the test case")
        val status: TestCaseStatus,
        @Schema(description = "Start timestamp")
        val startTimestamp: Instant?,
        @Schema(description = "End timestamp")
        val endTimestamp: Instant?,
        @Schema(description = "Operation metrics by operation ID")
        val operationMetrics: Map<OperationId, TargetTestCaseOperationMetrics>,
        @Schema(description = "Relative domain metric")
        val relativeDomainMetric: BigDecimal?,
        @Schema(description = "K6 output")
        val k6Output: String?,
        @Schema(description = "JSON summary of the K6 run")
        val jsonSummary: JsonNode?,
    )
}
