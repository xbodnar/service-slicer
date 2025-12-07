package cz.bodnor.serviceslicer.adapter.`in`.web.benchmarkrun

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.domain.benchmarkrun.ExperimentResults
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCaseOperationMetrics
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import cz.bodnor.serviceslicer.domain.testcase.TargetTestCaseOperationMetrics
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Schema(description = "List of benchmark runs")
data class ListBenchmarkRunsResponse(
    val items: List<BenchmarkRunDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

@Schema(description = "Benchmark run DTO")
data class BenchmarkRunDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val benchmarkId: UUID,
    val testDuration: String,
    val status: JobStatus,
    val baselineTestCase: BaselineTestCaseDto,
    val targetTestCases: List<TargetTestCaseDto>,
    val experimentResults: ExperimentResults?,
)

@Schema(description = "Baseline test case DTO")
data class BaselineTestCaseDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val load: Int,
    val startTimestamp: Instant?,
    val endTimestamp: Instant?,
    val status: JobStatus,
    val k6Output: String?,
    val jsonSummary: JsonNode?,
    val baselineSutId: UUID,
    val operationMetrics: Map<OperationId, BaselineTestCaseOperationMetrics>,
    val relativeDomainMetrics: Map<Int, BigDecimal>,
)

@Schema(description = "Target test case DTO")
data class TargetTestCaseDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val load: Int,
    val startTimestamp: Instant?,
    val endTimestamp: Instant?,
    val status: JobStatus,
    val k6Output: String?,
    val jsonSummary: JsonNode?,
    val targetSutId: UUID,
    val loadFrequency: BigDecimal,
    val operationMetrics: Map<OperationId, TargetTestCaseOperationMetrics>,
    val relativeDomainMetric: BigDecimal?,
)
