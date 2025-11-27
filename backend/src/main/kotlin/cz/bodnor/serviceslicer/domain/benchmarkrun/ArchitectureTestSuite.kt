package cz.bodnor.serviceslicer.domain.benchmarkrun

import java.util.UUID

data class ArchitectureTestSuite(
    val targetSutId: UUID,
    var status: TestSuiteStatus = TestSuiteStatus.PENDING,
    val targetTestCases: MutableList<TargetTestCase> = mutableListOf(),
) {
    fun addTestCase(load: Int) = targetTestCases.add(TargetTestCase(load))

    fun updateOverallStatus() {
        status = when {
            targetTestCases.any { it.status == TestCaseStatus.FAILED } -> TestSuiteStatus.FAILED
            targetTestCases.all { it.status == TestCaseStatus.COMPLETED } -> TestSuiteStatus.COMPLETED
            targetTestCases.any { it.status == TestCaseStatus.RUNNING } -> TestSuiteStatus.RUNNING
            else -> TestSuiteStatus.PENDING
        }
    }
}
