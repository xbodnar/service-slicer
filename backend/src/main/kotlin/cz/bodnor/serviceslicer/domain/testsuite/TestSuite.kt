package cz.bodnor.serviceslicer.domain.testsuite

import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import cz.bodnor.serviceslicer.domain.testcase.TestCase
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.util.UUID
import kotlin.collections.get

@Entity
class TestSuite(
    @ManyToOne
    val benchmarkRun: BenchmarkRun,

    @ManyToOne
    val systemUnderTest: SystemUnderTest,

    val isBaseline: Boolean,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        private set

    var startTimestamp: Instant? = null
        private set

    var endTimestamp: Instant? = null
        private set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "testSuite", fetch = FetchType.EAGER)
    private val _testCases: MutableList<TestCase> = mutableListOf()

    val testCases: Set<TestCase>
        get() = _testCases.toSet()

    @JdbcTypeCode(SqlTypes.JSON)
    var testSuiteResults: TestSuiteResults? = null
        private set

    fun started() {
        this.status = JobStatus.RUNNING
        this.startTimestamp = Instant.now()
    }

    fun failed() {
        this.status = JobStatus.FAILED
        this.endTimestamp = Instant.now()
    }

    fun queued() {
        require(this.status == JobStatus.FAILED) { "Cannot queue test suite in status $status" }
        this.status = JobStatus.PENDING
        this.startTimestamp = null
        this.endTimestamp = null
        this._testCases.filter { it.status == JobStatus.FAILED }.forEach { it.queued() }
    }

    fun completed(scalabilityThresholds: Map<OperationId, BigDecimal>) {
        this.status = JobStatus.COMPLETED
        this.endTimestamp = Instant.now()
        this.testSuiteResults = computeExperimentResults(scalabilityThresholds)
    }

    fun addTestCase(
        load: Int,
        frequency: BigDecimal,
    ) {
        _testCases.add(TestCase(this, load, frequency))
    }

    fun computeExperimentResults(scalabilityThresholds: Map<OperationId, BigDecimal>): TestSuiteResults {
        val operations = benchmarkRun.benchmark.operationalSetting.usageProfile.flatMap {
            it.steps.map { step -> OperationId(step.operationId) }
        }.toSet()
        val operationExperimentResults = operations.associateWith { operation ->
            val gsl = findGsl(operation)
            val scalabilityGap = gsl?.let { computeScalabilityGap(operation, it) }
            val scalabilityThreshold =
                scalabilityThresholds[operation] ?: error("Missing scalability threshold for $operation")
            val performanceOffset = gsl?.let { computePerformanceOffset(scalabilityThreshold, operation, it) }

            OperationExperimentResults(
                operationId = operation.value,
                totalRequests = testCases.sumOf { it.operationMetrics[operation]?.totalRequests ?: 0L },
                failedRequests = testCases.sumOf { it.operationMetrics[operation]?.failedRequests ?: 0L },
                scalabilityFootprint = gsl,
                scalabilityGap = scalabilityGap,
                performanceOffset = performanceOffset,
            )
        }

        return TestSuiteResults(
            totalDomainMetric = testCases.sumOf { it.relativeDomainMetric!! },
            operationExperimentResults = operationExperimentResults,
        )
    }

    /**
     * Finds the greatest successful load (GSL) for the given operation.
     */
    private fun findGsl(operation: OperationId): Int? {
        var gsl: Int? = null
        testCases.sortedBy { it.load }.forEach { testCase ->
            if (testCase.operationMetrics[operation]?.passScalabilityThreshold == true) {
                gsl = testCase.load
            }
        }

        return gsl
    }

    private fun computeScalabilityGap(
        operation: OperationId,
        gsl: Int,
    ): BigDecimal? {
        val firstFailingLoad = testCases
            .sortedBy { it.load }
            .filter { it.load > gsl }
            .firstOrNull { it.operationMetrics[operation]?.passScalabilityThreshold == false }
        if (firstFailingLoad == null) {
            return null
        }

        return firstFailingLoad.operationMetrics[operation]?.invocationFrequency
    }

    private fun computePerformanceOffset(
        scalabilityThreshold: BigDecimal,
        operation: OperationId,
        load: Int,
    ): BigDecimal? {
        val firstFailingLoad = testCases
            .sortedBy { it.load }
            .filter { it.load > load }
            .firstOrNull { it.operationMetrics[operation]?.passScalabilityThreshold == false }

        if (firstFailingLoad == null) {
            return null
        }

        val mu = firstFailingLoad.operationMetrics[operation]?.meanResponseTimeMs ?: return null

        // Relative overshoot (e.g. 0.5 = 50% slower than threshold)
        return (mu - scalabilityThreshold).divide(scalabilityThreshold, MathContext.DECIMAL32)
    }
}

@Repository
interface TestSuiteRepository : JpaRepository<TestSuite, UUID>
