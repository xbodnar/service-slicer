package cz.bodnor.serviceslicer.domain.testcase

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant

@Entity
class TargetTestCase(
    @ManyToOne
    val benchmarkRun: BenchmarkRun,

    @ManyToOne
    val targetSut: SystemUnderTest,

    load: Int,

    val loadFrequency: BigDecimal,
) : TestCase(load) {

    @JdbcTypeCode(SqlTypes.JSON)
    var operationMetrics: Map<OperationId, TargetTestCaseOperationMetrics> = emptyMap()
        private set

    var relativeDomainMetric: BigDecimal? = null
        private set

    override fun completed(
        performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
    ) {
        require(benchmarkRun.baselineTestCase.status == TestCaseStatus.COMPLETED) {
            "Baseline test case must be completed before target test case"
        }
        this.status = TestCaseStatus.COMPLETED
        this.endTimestamp = Instant.now()

        val totalRequests = performanceMetrics.sumOf { it.totalRequests }

        this.operationMetrics = performanceMetrics.map { metrics ->
            val operationId = OperationId(metrics.operationId)
            val baselineMetrics = benchmarkRun.baselineTestCase.operationMetrics[operationId]
                ?: error("No baseline metrics found for operation $operationId")

            val passScalabilityThreshold = metrics.meanResponseTimeMs <= baselineMetrics.scalabilityThreshold
            val invocationFreq = when (totalRequests) {
                0L -> BigDecimal.ZERO
                else -> metrics.totalRequests.toBigDecimal().divide(totalRequests.toBigDecimal(), MathContext.DECIMAL32)
            }
            val scalabilityShare = when (passScalabilityThreshold) {
                true -> invocationFreq
                false -> BigDecimal.ZERO
            }

            TargetTestCaseOperationMetrics(
                operationId = operationId,
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
        this.relativeDomainMetric = computeRelativeDomainMetric()
        this.k6Output = k6Output
        this.jsonSummary = jsonSummary
    }

    private fun computeRelativeDomainMetric(): BigDecimal {
        val sumShares = operationMetrics.values.sumOf { it.scalabilityShare }
        return loadFrequency.multiply(sumShares)
    }
}
