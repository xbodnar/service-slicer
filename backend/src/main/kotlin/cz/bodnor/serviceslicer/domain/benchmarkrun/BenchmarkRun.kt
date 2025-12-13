package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import cz.bodnor.serviceslicer.domain.testcase.TestCase
import cz.bodnor.serviceslicer.domain.testsuite.TestSuite
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration

@Entity
class BenchmarkRun(
    @ManyToOne
    val benchmark: Benchmark,

    val testDuration: Duration,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        private set

    var startTimestamp: Instant? = null
        private set

    var endTimestamp: Instant? = null
        private set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "benchmarkRun", fetch = FetchType.EAGER)
    private val _testSuites: MutableList<TestSuite> = mutableListOf()

    val testSuites: Set<TestSuite>
        get() = _testSuites.toSet()

    fun getTestDurationString() = testDuration.toString()

    fun addTestSuite(
        systemUnderTest: SystemUnderTest,
        isBaseline: Boolean,
    ): TestSuite {
        val testSuite = TestSuite(benchmarkRun = this, systemUnderTest = systemUnderTest, isBaseline = isBaseline)
        this._testSuites.add(testSuite)
        return testSuite
    }

    fun queue() {
        require(this.status == JobStatus.FAILED) { "Cannot queue benchmark run in status $status" }
        this.status = JobStatus.PENDING
        this.startTimestamp = null
        this.endTimestamp = null
        this._testSuites.filter { it.status == JobStatus.FAILED }.forEach { it.queued() }
    }

    fun started() {
        this.status = JobStatus.RUNNING
    }

    fun completed() {
        this.status = JobStatus.COMPLETED
    }

    fun failed() {
        this.status = JobStatus.FAILED
    }

    fun getBaselineTestCase(): TestCase {
        val baselineTestSuite = testSuites.first { it.isBaseline }
        return baselineTestSuite.testCases.minBy { it.load }
    }

    fun getScalabilityThresholds(): Map<OperationId, BigDecimal> {
        val baselineTestCase = getBaselineTestCase()
        require(baselineTestCase.status == JobStatus.COMPLETED) {
            "Baseline test case is not completed yet, status: ${baselineTestCase.status}"
        }
        return baselineTestCase.operationMetrics.mapValues { (_, metrics) ->
            metrics.meanResponseTimeMs + metrics.stdDevResponseTimeMs.multiply(3.toBigDecimal())
        }
    }
}

@Repository
interface BenchmarkRunRepository : JpaRepository<BenchmarkRun, UUID> {
    fun findAllByBenchmarkId(
        benchmarkId: UUID,
        pageable: Pageable,
    ): Page<BenchmarkRun>

    fun findFirstByStatusOrderByCreatedTimestampAsc(status: JobStatus): BenchmarkRun?
}
