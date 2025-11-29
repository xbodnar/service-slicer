package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

@Entity
class TargetTestCase(
    val load: Int,
    val loadFrequency: Double,
    @ManyToOne(fetch = FetchType.LAZY)
    val architectureTestSuite: ArchitectureTestSuite,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: TestCaseStatus = TestCaseStatus.RUNNING
        private set

    var startTimestamp: Instant? = Instant.now()
        private set

    var endTimestamp: Instant? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var operationMeasurements: Map<OperationId, OperationMetrics> = emptyMap()
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var passScalabilityThreshold: Map<OperationId, Boolean> = emptyMap()
        private set

    // estimates the conditional probability that operation succeeds given deployment architecture α and load λ.
    @JdbcTypeCode(SqlTypes.JSON)
    var scalabilityShares: Map<OperationId, BigDecimal> = emptyMap()
        private set

    var relativeDomainMetric: BigDecimal? = null
        private set

    @Column(name = "k6_output")
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
            val baselineScalabilityThreshold =
                baselineTestCase.scalabilityThresholds[it.key]
                    ?: error("No baseline scalability threshold found for operation ${it.key}")
            it.value.meanResponseTimeMs <= baselineScalabilityThreshold
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
