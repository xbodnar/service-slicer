package cz.bodnor.serviceslicer.domain.testcase

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.testsuite.TestSuite
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant

@Entity
class TestCase(
    @ManyToOne
    val testSuite: TestSuite,

    val load: Int,

    val loadFrequency: BigDecimal,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        private set

    var startTimestamp: Instant? = null
        private set

    var endTimestamp: Instant? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var operationMetrics: Map<OperationId, TestCaseOperationMetrics> = emptyMap()
        private set

    @Column(name = "k6_output")
    var k6Output: String? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var jsonSummary: JsonNode? = null
        private set

    var relativeDomainMetric: BigDecimal? = null
        private set

    fun started() {
        this.status = JobStatus.RUNNING
        this.startTimestamp = Instant.now()
    }

    fun queued() {
        require(this.status == JobStatus.FAILED) { "Cannot queue test case in status $status" }
        this.status = JobStatus.PENDING
        this.startTimestamp = null
        this.endTimestamp = null
    }
    fun failed() {
        this.status = JobStatus.FAILED
        this.endTimestamp = Instant.now()
    }

    fun completed(
        performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
        // Can be null only if this is the baseline test case
        scalabilityThresholds: Map<OperationId, BigDecimal>?,
    ) {
        this.status = JobStatus.COMPLETED
        this.endTimestamp = Instant.now()
        require(testSuite.isBaseline || scalabilityThresholds != null) {
            "Scalability thresholds must be provided for target test case"
        }

        val totalRequests = performanceMetrics.sumOf { it.totalRequests }

        this.operationMetrics = performanceMetrics.map { metrics ->
            val passScalabilityThreshold = when (scalabilityThresholds) {
                null -> true

                else -> metrics.meanResponseTimeMs <= (
                    scalabilityThresholds[metrics.operationId]
                        ?: error("Scalability thresholds not found for operation: ${metrics.operationId}")
                    )
            }
            val invocationFreq = when (totalRequests) {
                0L -> BigDecimal.ZERO
                else -> metrics.totalRequests.toBigDecimal().divide(totalRequests.toBigDecimal(), MathContext.DECIMAL32)
            }
            val scalabilityShare = when (passScalabilityThreshold) {
                true -> invocationFreq
                false -> BigDecimal.ZERO
            }

            TestCaseOperationMetrics(
                operationId = metrics.operationId,
                totalRequests = metrics.totalRequests,
                failedRequests = metrics.failedRequests,
                invocationFrequency = invocationFreq,
                meanResponseTimeMs = metrics.meanResponseTimeMs,
                stdDevResponseTimeMs = metrics.stdDevResponseTimeMs,
                p95DurationMs = metrics.p95DurationMs,
                p99DurationMs = metrics.p99DurationMs,
                passScalabilityThreshold = passScalabilityThreshold,
                scalabilityShare = scalabilityShare,
            )
        }.associateBy { it.operationId }

        this.k6Output = k6Output
        this.jsonSummary = jsonSummary
        this.relativeDomainMetric = computeRelativeDomainMetric()
    }

    private fun computeRelativeDomainMetric(): BigDecimal {
        val scalabilityShares = operationMetrics.values.map { it.scalabilityShare }
        val sumShares = scalabilityShares.sumOf { it }
        return loadFrequency.multiply(sumShares)
    }
}
