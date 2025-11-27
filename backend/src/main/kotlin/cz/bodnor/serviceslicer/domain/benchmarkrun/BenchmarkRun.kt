package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.benchmark.OperationalLoad
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
class BenchmarkRun(
    val benchmarkId: UUID,
    @JdbcTypeCode(SqlTypes.JSON)
    val architectureTestSuites: MutableList<ArchitectureTestSuite> = mutableListOf(),
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var state: BenchmarkRunState = BenchmarkRunState.PENDING
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var baselineTestCase: BaselineTestCase? = null
        private set

    fun addTestCase(
        isBaseline: Boolean,
        sutId: UUID,
        load: OperationalLoad,
    ) {
        when (isBaseline) {
            true -> this.baselineTestCase = BaselineTestCase(sutId, load.load)

            false -> {
                val testSuite = getOrCreateTestSuite(sutId)
                val testCase = testSuite.addTestCase(load)
                testSuite.updateOverallStatus()
            }
        }
    }

    fun markTestCaseCompleted(
        sutId: UUID,
        load: Int,
        endTimestamp: Instant,
        measurements: List<OperationMetrics>,
        k6Output: String,
    ) {
        if (baselineTestCase?.baselineSutId == sutId) {
            baselineTestCase!!.markCompleted(endTimestamp, measurements, k6Output)
        } else {
            val (testSuite, testCase) = getTargetTestCase(sutId, load)
            testCase.markCompleted(baselineTestCase!!, endTimestamp, measurements, k6Output)
            testSuite.updateOverallStatus()
        }
    }

    fun markTestCaseFailed(
        sutId: UUID,
        load: Int,
        endTimestamp: Instant,
    ) {
        if (baselineTestCase?.baselineSutId == sutId) {
            baselineTestCase!!.markFailed(endTimestamp)
        } else {
            val (testSuite, testCase) = getTargetTestCase(sutId, load)
            testCase.markFailed(endTimestamp)
            testSuite.updateOverallStatus()
        }
    }

    fun markFailed() {
        state = BenchmarkRunState.FAILED
    }

    fun markCompleted() {
        state = BenchmarkRunState.COMPLETED
    }

    fun getOrCreateTestSuite(sutId: UUID): ArchitectureTestSuite = architectureTestSuites
        .find { it.targetSutId == sutId }
        ?: ArchitectureTestSuite(targetSutId = sutId).also { architectureTestSuites.add(it) }

    fun getNextTestCaseToRun(benchmark: Benchmark): TestCaseToRun? {
        if (baselineTestCase == null) {
            return TestCaseToRun(
                sutId = benchmark.systemsUnderTest.find { it.isBaseline }?.id
                    ?: error("No baseline SUT found in benchmark $benchmarkId"),
                load = benchmark.config.operationalProfile.minBy { it.load },
                isBaseline = true,
            )
        }

        return benchmark.systemsUnderTest
            .filter { !it.isBaseline }
            .flatMap { sut ->
                benchmark.config.operationalProfile.map { profile ->
                    TestCaseToRun(
                        sutId = sut.id,
                        load = profile,
                        isBaseline = false,
                    )
                }
            }
            .firstOrNull { input ->
                val testSuite = architectureTestSuites.find {
                    it.targetSutId == input.sutId
                }
                val testCase = testSuite?.targetTestCases?.find { it.load == input.load.load }

                testCase == null
            }
    }

    private fun getTargetTestCase(
        sutId: UUID,
        load: Int,
    ): Pair<ArchitectureTestSuite, TargetTestCase> {
        val testSuite = architectureTestSuites.find { it.targetSutId == sutId }
            ?: error("Test suite for SUT $sutId not found")
        val testCase = testSuite.targetTestCases.find { it.load == load }
            ?: error("Test case for SUT $sutId with load $load not found")

        return testSuite to testCase
    }

    data class TestCaseToRun(
        val sutId: UUID,
        val load: OperationalLoad,
        val isBaseline: Boolean,
    )
}

@Repository
interface BenchmarkRunRepository : JpaRepository<BenchmarkRun, UUID> {
    fun findAllByBenchmarkId(benchmarkId: UUID): List<BenchmarkRun>
}

enum class BenchmarkRunState {
    PENDING,
    COMPLETED,
    FAILED,
}
