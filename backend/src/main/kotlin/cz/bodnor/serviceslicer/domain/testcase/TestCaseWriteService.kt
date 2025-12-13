package cz.bodnor.serviceslicer.domain.testcase

import org.springframework.stereotype.Service

@Service
class TestCaseWriteService(
    private val repository: TestCaseRepository,
) {

    fun save(testCase: TestCase) = repository.save(testCase)
}
