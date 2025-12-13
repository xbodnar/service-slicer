package cz.bodnor.serviceslicer.domain.testsuite

import org.springframework.stereotype.Service

@Service
class TestSuiteWriteService(
    private val repository: TestSuiteRepository,
) {

    fun save(testSuite: TestSuite) = repository.save(testSuite)
}
