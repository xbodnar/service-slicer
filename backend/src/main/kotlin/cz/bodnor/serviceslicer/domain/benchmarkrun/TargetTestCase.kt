package cz.bodnor.serviceslicer.domain.benchmarkrun

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
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
    var operationMetrics: Map<OperationId, TargetTestCaseOperationMetrics> = emptyMap()
        private set

    var relativeDomainMetric: BigDecimal? = null
        private set

    @Column(name = "k6_output")
    var k6Output: String? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var jsonSummary: JsonNode? = null
        private set

    fun markCompleted(
        baselineTestCase: BaselineTestCase,
        endTime: Instant,
        performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
    ) {
        this.status = TestCaseStatus.COMPLETED
        this.endTimestamp = endTime

        val totalRequests = performanceMetrics.sumOf { it.totalRequests }

        this.operationMetrics = performanceMetrics.map { metrics ->
            val operationId = OperationId(metrics.operationId)
            val baselineMetrics = baselineTestCase.operationMetrics[operationId]
                ?: error("No baseline metrics found for operation $operationId")
            val passScalabilityThreshold = metrics.meanResponseTimeMs <= baselineMetrics.scalabilityThreshold

            TargetTestCaseOperationMetrics(
                operationId = operationId,
                totalRequests = metrics.totalRequests,
                failedRequests = metrics.failedRequests,
                meanResponseTimeMs = metrics.meanResponseTimeMs,
                stdDevResponseTimeMs = metrics.stdDevResponseTimeMs,
                p95DurationMs = metrics.p95DurationMs,
                p99DurationMs = metrics.p99DurationMs,
                passScalabilityThreshold = passScalabilityThreshold,
                scalabilityShare = calculateScalabilityShare(
                    operationMetrics = metrics,
                    totalTargetTestCaseRequests = totalRequests,
                    passScalabilityThreshold = passScalabilityThreshold,
                ),
            )
        }.associateBy { it.operationId }
        this.relativeDomainMetric =
            loadFrequency.toBigDecimal() * this.operationMetrics.values.sumOf { it.scalabilityShare }
        this.k6Output = k6Output
        this.jsonSummary = jsonSummary
    }

    private fun calculateScalabilityShare(
        operationMetrics: QueryLoadTestMetrics.PerformanceMetrics,
        totalTargetTestCaseRequests: Long,
        passScalabilityThreshold: Boolean,
    ): BigDecimal {
        if (!passScalabilityThreshold) {
            return BigDecimal.ZERO
        }

        val numerator = operationMetrics.totalRequests.toBigDecimal()
        val denominator = totalTargetTestCaseRequests.toBigDecimal()

        return numerator.divide(denominator, 8, RoundingMode.HALF_UP)
    }

    fun markFailed(endTime: Instant) {
        this.status = TestCaseStatus.FAILED
        this.endTimestamp = endTime
    }
}
