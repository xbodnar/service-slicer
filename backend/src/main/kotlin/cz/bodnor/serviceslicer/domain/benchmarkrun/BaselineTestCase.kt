package cz.bodnor.serviceslicer.domain.benchmarkrun

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * Represents the baseline requirements
 */
data class BaselineTestCase(
    val baselineSutId: UUID,
    val load: Int,
) {
    var status: TestCaseStatus = TestCaseStatus.RUNNING
        private set

    var startTimestamp: Instant = Instant.now()
        private set

    var endTimestamp: Instant? = null
        private set

    var operationMeasurements: Map<OperationId, OperationMetrics> = emptyMap()
        private set

    // The scalability threshold for each operation is meanResponseTime + 3 * stdDevResponseTime
    var scalabilityThresholds: Map<OperationId, BigDecimal> = emptyMap()
        private set

    var k6Output: String? = null
        private set

    fun markCompleted(
        endTime: Instant,
        measurements: List<OperationMetrics>,
        k6Output: String,
    ) {
        this.status = TestCaseStatus.COMPLETED
        this.endTimestamp = endTime
        this.operationMeasurements = measurements.associateBy { it.operationId }
        this.scalabilityThresholds =
            operationMeasurements.mapValues {
                it.value.meanResponseTimeMs +
                    it.value.stdDevResponseTimeMs.multiply(3.toBigDecimal())
            }
        this.k6Output = k6Output
    }

    fun markFailed(endTime: Instant) {
        this.status = TestCaseStatus.FAILED
        this.endTimestamp = endTime
    }
}
