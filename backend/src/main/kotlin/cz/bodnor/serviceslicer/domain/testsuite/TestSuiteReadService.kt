package cz.bodnor.serviceslicer.domain.testsuite

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class TestSuiteReadService(
    private val repository: TestSuiteRepository,
) : BaseFinderService<TestSuite>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = TestSuite::class
}
