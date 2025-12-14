package cz.bodnor.serviceslicer.adapter.`in`.web.benchmarkrun

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.adapter.`in`.web.sut.SystemUnderTestDto
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import cz.bodnor.serviceslicer.domain.testcase.TestCaseOperationMetrics
import cz.bodnor.serviceslicer.domain.testsuite.TestSuiteResults
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
    val usageProfile: List<BehaviorModel>,
    val operationalProfile: Map<Int, BigDecimal>,
    val status: JobStatus,
    val testSuites: List<TestSuiteDto>,
    val scalabilityThresholds: Map<OperationId, BigDecimal>?,
)

data class TestSuiteDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val systemUnderTest: SystemUnderTestDto,
    val isBaseline: Boolean,
    val status: JobStatus,
    val startTimestamp: Instant?,
    val endTimestamp: Instant?,
    val testSuiteResults: TestSuiteResults?,
    val testCases: List<TestCaseDto>,
)

@Schema(description = "Target test case DTO")
data class TestCaseDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val load: Int,
    val loadFrequency: BigDecimal,
    val status: JobStatus,
    val startTimestamp: Instant?,
    val endTimestamp: Instant?,
    val operationMetrics: Map<OperationId, TestCaseOperationMetrics>,
    val k6Output: String?,
    val jsonSummary: JsonNode?,
    val relativeDomainMetric: BigDecimal?,
)
