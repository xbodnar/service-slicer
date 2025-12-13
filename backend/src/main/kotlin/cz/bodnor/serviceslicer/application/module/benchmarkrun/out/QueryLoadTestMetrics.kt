package cz.bodnor.serviceslicer.application.module.benchmarkrun.out

import cz.bodnor.serviceslicer.domain.testcase.OperationId
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

interface QueryLoadTestMetrics {

    data class PerformanceMetrics(
        val operationId: OperationId,

        // counts
        val totalRequests: Long,
        val failedRequests: Long,

        // latency
        val meanResponseTimeMs: BigDecimal,
        val stdDevResponseTimeMs: BigDecimal,

        // percentiles
        val p95DurationMs: BigDecimal,
        val p99DurationMs: BigDecimal,
    )

    /**
     * Query load test metrics from Prometheus for a specific benchmark and SUT.
     *
     * @param testCaseId The test case ID to filter metrics
     * @param start The start timestamp of the load test run
     * @param end The end timestamp of the load test run
     * @return List of operation-level metrics aggregated by operation ID and component
     */
    operator fun invoke(
        testCaseId: UUID,
        start: Instant,
        end: Instant,
    ): List<PerformanceMetrics>
}
