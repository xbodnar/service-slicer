package cz.bodnor.serviceslicer.domain.sut

import org.springframework.stereotype.Service

@Service
class SystemUnderTestWriteService(
    private val repository: SystemUnderTestRepository,
) {

    fun save(systemUnderTest: SystemUnderTest) = repository.save(systemUnderTest)
}
