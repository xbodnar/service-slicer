package cz.bodnor.serviceslicer.domain.benchmarkrun

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCase
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import cz.bodnor.serviceslicer.domain.testcase.TargetTestCase
import cz.bodnor.serviceslicer.domain.testcase.TestCase
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.MathContext
import java.util.UUID
import kotlin.time.Duration

@Entity
class BenchmarkRun(
    @ManyToOne
    val benchmark: Benchmark,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    val baselineTestCase: BaselineTestCase,

    val testDuration: Duration,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        private set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "benchmarkRun", fetch = FetchType.EAGER)
    val targetTestCases: MutableList<TargetTestCase> = mutableListOf()

    @JdbcTypeCode(SqlTypes.JSON)
    var experimentResults: ExperimentResults? = null
        private set

    fun getTestDurationString() = testDuration.toString()

    fun addTargetTestCase(
        sut: SystemUnderTest,
        load: Int,
        frequency: BigDecimal,
    ) {
        this.targetTestCases.add(TargetTestCase(this, sut, load, frequency))
    }

    fun markTestCaseCompleted(
        testCaseId: UUID,
        performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
    ) {
        if (baselineTestCase.id == testCaseId) {
            baselineTestCase.completed(performanceMetrics, k6Output, jsonSummary)
        } else {
            getTargetTestCase(testCaseId).completed(performanceMetrics, k6Output, jsonSummary)
        }
        this.updateOverallStatus()
    }

    fun markTestCaseFailed(testCaseId: UUID) {
        if (baselineTestCase.id == testCaseId) {
            baselineTestCase.failed()
        } else {
            getTargetTestCase(testCaseId).failed()
        }
        this.updateOverallStatus()
    }

    private fun updateOverallStatus() {
        val allTestCases = listOf(baselineTestCase) + targetTestCases
        this.status = when {
            allTestCases.any { it.status == JobStatus.FAILED } -> JobStatus.FAILED
            allTestCases.all { it.status == JobStatus.COMPLETED } -> JobStatus.COMPLETED
            allTestCases.any { it.status == JobStatus.RUNNING } -> JobStatus.RUNNING
            else -> JobStatus.PENDING
        }

        if (this.status == JobStatus.COMPLETED) {
            this.experimentResults = computeExperimentResults()
        }
    }

    fun computeExperimentResults(): ExperimentResults {
        val operations = targetTestCases.flatMap { it.operationMetrics.keys }
        val operationExperimentResults = operations.associateWith { operation ->
            val gsl = findGsl(operation)
            val scalabilityGap = gsl?.let { computeScalabilityGap(operation, it) }
            val performanceOffset = gsl?.let { computePerformanceOffset(operation, it) }

            OperationExperimentResults(
                operationId = operation.value,
                totalRequests = targetTestCases.sumOf { it.operationMetrics[operation]?.totalRequests ?: 0L },
                failedRequests = targetTestCases.sumOf { it.operationMetrics[operation]?.failedRequests ?: 0L },
                scalabilityFootprint = gsl,
                scalabilityGap = scalabilityGap,
                performanceOffset = performanceOffset,
            )
        }

        return ExperimentResults(
            totalDomainMetric = targetTestCases.sumOf { it.relativeDomainMetric!! },
            operationExperimentResults = operationExperimentResults,
        )
    }

    private fun computePerformanceOffset(
        operation: OperationId,
        load: Int,
    ): BigDecimal? {
        val firstFailingLoad = targetTestCases
            .sortedBy { it.load }
            .filter { it.load > load }
            .firstOrNull { it.operationMetrics[operation]?.passScalabilityThreshold == false }

        if (firstFailingLoad == null) {
            return null
        }

        val mu = firstFailingLoad.operationMetrics[operation]?.meanResponseTimeMs ?: return null

        // Relative overshoot (e.g. 0.5 = 50% slower than threshold)
        return (mu - baselineTestCase.operationMetrics[operation]?.scalabilityThreshold!!).divide(
            baselineTestCase.operationMetrics[operation]?.scalabilityThreshold!!,
            MathContext.DECIMAL32,
        )
    }

    private fun computeScalabilityGap(
        operation: OperationId,
        gsl: Int,
    ): BigDecimal? {
        val firstFailingLoad = targetTestCases
            .sortedBy { it.load }
            .filter { it.load > gsl }
            .firstOrNull { it.operationMetrics[operation]?.passScalabilityThreshold == false }
        if (firstFailingLoad == null) {
            return null
        }

        return firstFailingLoad.operationMetrics[operation]?.invocationFrequency
    }

    /**
     * Finds the greatest successful load (GSL) for the given operation.
     */
    private fun findGsl(operation: OperationId): Int? {
        var gsl: Int? = null
        targetTestCases.sortedBy { it.load }.forEach { testCase ->
            if (testCase.operationMetrics[operation]?.passScalabilityThreshold == true) {
                gsl = testCase.load
            }
        }

        return gsl
    }

    private fun getTargetTestCase(id: UUID): TargetTestCase = targetTestCases.find { it.id == id }
        ?: error("Test case with id $id not found")

    fun getNextTestCaseToRun(): TestCase? {
        if (baselineTestCase.status == JobStatus.PENDING) {
            return baselineTestCase
        }

        return targetTestCases.sortedBy { it.load }.firstOrNull { it.status == JobStatus.PENDING }
    }
}

@Repository
interface BenchmarkRunRepository : JpaRepository<BenchmarkRun, UUID> {
    fun findAllByBenchmarkId(
        benchmarkId: UUID,
        pageable: Pageable,
    ): Page<BenchmarkRun>
}
