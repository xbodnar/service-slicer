package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmark.OperationalLoad
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
class ArchitectureTestSuite(
    val targetSutId: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    var benchmarkRun: BenchmarkRun,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: TestSuiteStatus = TestSuiteStatus.PENDING
        private set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "architecture_test_suite_id")
    val targetTestCases: MutableList<TargetTestCase> = mutableListOf()

    /**
     * The scalability footprint measures the scalability level of each operation
     * exposed by the SUT. To obtain it, we first define the Greatest Successful Load (GSL) ˆλj for an operation
     * oj as the greatest load for which oj succeeds. This implies that oj fails for all λ > ˆλj.
     * When ˆλj equals the maximum load in Λ, oj exhibits optimal scalability. The set of GSLs for all operations
     * is referred to as Scalability Footprint Λˆ α of the SUT
     */
    @JdbcTypeCode(SqlTypes.JSON)
    val scalabilityFootprint: MutableMap<OperationId, Int> = mutableMapOf()

    fun addTestCase(operationalLoad: OperationalLoad) {
        targetTestCases.add(TargetTestCase(operationalLoad.load, operationalLoad.frequency, this))
    }

    fun updateOverallStatus() {
        status = when {
            targetTestCases.any { it.status == TestCaseStatus.FAILED } -> TestSuiteStatus.FAILED
            targetTestCases.all { it.status == TestCaseStatus.COMPLETED } -> TestSuiteStatus.COMPLETED
            targetTestCases.any { it.status == TestCaseStatus.RUNNING } -> TestSuiteStatus.RUNNING
            else -> TestSuiteStatus.PENDING
        }

        if (status == TestSuiteStatus.COMPLETED) {
            computeScalabilityFootprint()
        }
    }

    private fun computeScalabilityFootprint() {
        val operations = targetTestCases.flatMap { it.operationMeasurements.keys }
        operations.forEach { operation ->
            var gsl = 0
            targetTestCases.forEach { testCase ->
                if (testCase.passScalabilityThreshold[operation]!! && testCase.load > gsl) {
                    gsl = testCase.load
                }
            }
            scalabilityFootprint[operation] = gsl
        }
    }
}
