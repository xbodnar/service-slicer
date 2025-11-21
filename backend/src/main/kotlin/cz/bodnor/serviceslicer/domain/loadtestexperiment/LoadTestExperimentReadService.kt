package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service
import java.lang.IllegalStateException
import java.util.UUID

@Service
class LoadTestExperimentReadService(
    private val repository: LoadTestExperimentRepository,
    private val sutRepository: SystemUnderTestRepository,
) : BaseFinderService<LoadTestExperiment>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = LoadTestExperiment::class

    fun getSystemUnderTestById(
        experimentId: UUID,
        sutId: UUID,
    ): SystemUnderTest {
        val sut = sutRepository.findById(sutId).orElseThrow {
            IllegalStateException("SystemUnderTest with id $sutId not found")
        }
        require(sut.experimentId == experimentId) {
            "SystemUnderTest with id $sutId does not belong to experiment $experimentId"
        }

        return sut
    }
}
