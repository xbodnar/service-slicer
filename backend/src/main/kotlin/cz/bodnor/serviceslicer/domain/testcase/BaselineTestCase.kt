package cz.bodnor.serviceslicer.domain.testcase

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant

/**
 * Represents the baseline requirements
 */
@Entity
class BaselineTestCase(
    @ManyToOne
    val baselineSut: SystemUnderTest,
    operationalProfile: Map<Int, BigDecimal>,
) : TestCase(operationalProfile.keys.min()) {

    @JdbcTypeCode(SqlTypes.JSON)
    var operationMetrics: Map<OperationId, BaselineTestCaseOperationMetrics> = emptyMap()
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    val relativeDomainMetrics: Map<Int, BigDecimal> = operationalProfile

    override fun completed(
        performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
    ) {
        this.status = TestCaseStatus.COMPLETED
        this.endTimestamp = Instant.now()
        this.operationMetrics = performanceMetrics.map { metrics ->
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
}
