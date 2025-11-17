package cz.bodnor.serviceslicer.domain.loadtestexperiment

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoadTestExperimentWriteService(
    private val experimentRepository: LoadTestExperimentRepository,
    private val systemUnderTestRepository: SystemUnderTestRepository,
) {

    fun create(
        loadTestConfigId: UUID,
        name: String,
        description: String? = null,
    ): LoadTestExperiment {
        val experiment = LoadTestExperiment(
            loadTestConfigId = loadTestConfigId,
            name = name,
            description = description,
        )

        return experimentRepository.save(experiment)
    }

    fun deleteSystemUnderTest(
        experimentId: UUID,
        sutId: UUID,
    ) {
        val sut = systemUnderTestRepository.findById(sutId)
            .orElseThrow { IllegalArgumentException("SystemUnderTest with id $sutId not found") }

        require(sut.experimentId == experimentId) {
            "SystemUnderTest with id $sutId does not belong to experiment $experimentId"
        }

        systemUnderTestRepository.delete(sut)
    }
}
