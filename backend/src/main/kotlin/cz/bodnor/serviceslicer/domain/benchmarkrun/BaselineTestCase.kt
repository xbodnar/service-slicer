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
        this.k6Output = k6Output
    }

    fun markFailed(endTime: Instant) {
        this.status = TestCaseStatus.FAILED
        this.endTimestamp = endTime
    }

    fun getOperationResponseTimeThreshold(operationId: OperationId): BigDecimal {
        val operation = operationMeasurements[operationId] ?: error("Operation $operationId not found")
        return operation.meanResponseTimeMs + operation.stdDevResponseTimeMs.multiply(3.toBigDecimal())
    }
}
