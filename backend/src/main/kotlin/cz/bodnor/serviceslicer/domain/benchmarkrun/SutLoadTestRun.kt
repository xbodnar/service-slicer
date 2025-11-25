package cz.bodnor.serviceslicer.domain.benchmarkrun

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class SutLoadTestRun(
    val systemUnderTestId: UUID,
    var status: SutLoadTestRunStatus = SutLoadTestRunStatus.PENDING,
    val loadResults: MutableList<LoadResult> = mutableListOf(),
) {
    fun getOrCreateLoadResult(load: Int): LoadResult = loadResults.find { it.load == load }
        ?: LoadResult(load = load).also { loadResults.add(it) }

    fun updateOverallStatus() {
        status = when {
            loadResults.any { it.status == LoadResultStatus.FAILED } -> SutLoadTestRunStatus.FAILED
            loadResults.all { it.status == LoadResultStatus.COMPLETED } -> SutLoadTestRunStatus.COMPLETED
            loadResults.any { it.status == LoadResultStatus.RUNNING } -> SutLoadTestRunStatus.RUNNING
            else -> SutLoadTestRunStatus.PENDING
        }
    }
}

enum class SutLoadTestRunStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
}

data class LoadResult(
    val load: Int,
    var status: LoadResultStatus = LoadResultStatus.PENDING,
    var startTimestamp: Instant? = null,
    var endTimestamp: Instant? = null,
    var operationMeasurements: List<OperationRunMetrics> = emptyList(),
    var k6Output: String? = null,
) {
    fun markRunning(startTime: Instant) {
        status = LoadResultStatus.RUNNING
        startTimestamp = startTime
    }

    fun markCompleted(
        endTime: Instant,
        measurements: List<OperationRunMetrics>,
        k6Output: String,
    ) {
        this.status = LoadResultStatus.COMPLETED
        this.endTimestamp = endTime
        this.operationMeasurements = measurements
        this.k6Output = k6Output
    }

    fun markFailed(endTime: Instant) {
        this.status = LoadResultStatus.FAILED
        this.endTimestamp = endTime
    }
}

enum class LoadResultStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
}

data class OperationRunMetrics(
    val operationId: String,

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
