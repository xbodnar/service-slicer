package cz.bodnor.serviceslicer.application.module.benchmarkrun.query

import cz.bodnor.serviceslicer.domain.benchmarkrun.ArchitectureTestSuite
import cz.bodnor.serviceslicer.domain.benchmarkrun.BaselineTestCase
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunState
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
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
        val architectureTestSuites: List<ArchitectureTestSuite>,
        @Schema(description = "Baseline test case")
        val baselineTestCase: BaselineTestCase?,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: Instant,
    )
}
