package cz.bodnor.serviceslicer.domain.sut

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SystemUnderTestWriteService(
    private val repository: SystemUnderTestRepository,
) {

    fun save(systemUnderTest: SystemUnderTest) = repository.save(systemUnderTest)

    fun delete(systemUnderTest: UUID) {
        repository.deleteById(systemUnderTest)
    }
}
