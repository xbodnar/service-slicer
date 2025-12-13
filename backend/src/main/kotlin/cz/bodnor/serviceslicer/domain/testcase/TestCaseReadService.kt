package cz.bodnor.serviceslicer.domain.testcase

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestCaseReadService(
    private val repository: TestCaseRepository,
) : BaseFinderService<TestCase>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = TestCase::class

    fun findNextTestCaseToRun(benchmarkRunId: UUID): TestCase? = repository.findNextTestCaseToExecute(benchmarkRunId)
}
