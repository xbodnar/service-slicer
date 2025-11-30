package cz.bodnor.serviceslicer.domain.benchmarkrun

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Represents the baseline requirements
 */
@Entity
class BaselineTestCase(
    val baselineSutId: UUID,
    val load: Int,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: TestCaseStatus = TestCaseStatus.RUNNING
        private set

    var startTimestamp: Instant = Instant.now()
        private set

    var endTimestamp: Instant? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var operationMetrics: Map<OperationId, BaselineTestCaseOperationMetrics> = emptyMap()
        private set

    @Column(name = "k6_output")
    var k6Output: String? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var jsonSummary: JsonNode? = null
        private set

    fun markCompleted(
        endTime: Instant,
        measurements: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
    ) {
        this.status = TestCaseStatus.COMPLETED
        this.endTimestamp = endTime
        this.operationMetrics = measurements.map { metrics ->
            BaselineTestCaseOperationMetrics(
                operationId = OperationId(metrics.operationId),
                totalRequests = metrics.totalRequests,
                failedRequests = metrics.failedRequests,
                meanResponseTimeMs = metrics.meanResponseTimeMs,
                stdDevResponseTimeMs = metrics.stdDevResponseTimeMs,
                p95DurationMs = metrics.p95DurationMs,
                p99DurationMs = metrics.p99DurationMs,
                scalabilityThreshold =
                metrics.meanResponseTimeMs + metrics.stdDevResponseTimeMs.multiply(3.toBigDecimal()),
            )
        }.associateBy { it.operationId }
        this.k6Output = k6Output
        this.jsonSummary = jsonSummary
    }

    fun markFailed(endTime: Instant) {
        this.status = TestCaseStatus.FAILED
        this.endTimestamp = endTime
    }
}
