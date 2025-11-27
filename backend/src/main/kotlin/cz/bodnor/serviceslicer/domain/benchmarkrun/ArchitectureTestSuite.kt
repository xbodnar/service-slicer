package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmark.OperationalLoad
import java.util.UUID

data class ArchitectureTestSuite(
    val targetSutId: UUID,
) {
    var status: TestSuiteStatus = TestSuiteStatus.PENDING
        private set

    val targetTestCases: MutableList<TargetTestCase> = mutableListOf()

    /**
     * The scalability footprint measures the scalability level of each operation
     * exposed by the SUT. To obtain it, we first define the Greatest Successful Load (GSL) ˆλj for an operation
     * oj as the greatest load for which oj succeeds. This implies that oj fails for all λ > ˆλj.
     * When ˆλj equals the maximum load in Λ, oj exhibits optimal scalability. The set of GSLs for all operations
     * is referred to as Scalability Footprint Λˆ α of the SUT
     */
    val scalabilityFootprint: MutableMap<OperationId, Int> = mutableMapOf()

    fun addTestCase(operationalLoad: OperationalLoad) =
        targetTestCases.add(TargetTestCase(operationalLoad.load, operationalLoad.frequency))

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
