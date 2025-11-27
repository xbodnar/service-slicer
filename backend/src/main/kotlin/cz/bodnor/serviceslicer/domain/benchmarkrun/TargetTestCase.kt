package cz.bodnor.serviceslicer.domain.benchmarkrun

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

data class TargetTestCase(
    val load: Int,
    val loadFrequency: Double,
) {
    var status: TestCaseStatus = TestCaseStatus.RUNNING
        private set

    var startTimestamp: Instant? = Instant.now()
        private set

    var endTimestamp: Instant? = null
        private set

    var operationMeasurements: Map<OperationId, OperationMetrics> = emptyMap()
        private set

    var passScalabilityThreshold: Map<OperationId, Boolean> = emptyMap()
        private set

    // estimates the conditional probability that operation succeeds given deployment architecture α and load λ.
    var scalabilityShares: Map<OperationId, BigDecimal> = emptyMap()
        private set

    var relativeDomainMetric: BigDecimal? = null
        private set

    var k6Output: String? = null
        private set

    fun markCompleted(
        baselineTestCase: BaselineTestCase,
        endTime: Instant,
        measurements: List<OperationMetrics>,
        k6Output: String,
    ) {
        this.status = TestCaseStatus.COMPLETED
        this.endTimestamp = endTime
        this.operationMeasurements = measurements.associateBy { it.operationId }
        this.passScalabilityThreshold = operationMeasurements.mapValues {
            it.value.meanResponseTimeMs <= baselineTestCase.scalabilityThresholds[it.key]
        }
        this.scalabilityShares = operationMeasurements.mapValues { (operationId, operationMetrics) ->
            // frequency of invocation of operation o over all invocations to all operations * 1 if operation passes, 0 if it fails
            val numerator = operationMetrics.totalRequests.toBigDecimal()
            val denominator = operationMeasurements.values.sumOf { it.totalRequests }.toBigDecimal()
            val freqOfInvocation = numerator.divide(denominator, 8, RoundingMode.HALF_UP)
            val passed = if (passScalabilityThreshold[operationId]!!) BigDecimal.ONE else BigDecimal.ZERO

            freqOfInvocation * passed
        }
        this.relativeDomainMetric = loadFrequency.toBigDecimal() * scalabilityShares.values.sumOf { it }
        this.k6Output = k6Output
    }

    fun markFailed(endTime: Instant) {
        this.status = TestCaseStatus.FAILED
        this.endTimestamp = endTime
    }
}
