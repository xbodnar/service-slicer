package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigWriteService
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
        systemsUnderTest: List<SystemUnderTestInput> = emptyList(),
    ): LoadTestExperiment {
        val experiment = LoadTestExperiment(
            loadTestConfigId = loadTestConfigId,
            name = name,
            description = description,
        )

        systemsUnderTest.forEach { sut ->
            val systemUnderTest = SystemUnderTest(
                id = UUID.randomUUID(),
                experimentId = experiment.id,
                name = sut.name,
                composeFileId = sut.composeFileId,
                jarFileId = sut.jarFileId,
                description = sut.description,
                healthCheckPath = sut.healthCheckPath,
                appPort = sut.appPort,
                startupTimeoutSeconds = sut.startupTimeoutSeconds,
            )
            experiment.addSystemUnderTest(systemUnderTest)
        }

        return experimentRepository.save(experiment)
    }

    fun addSystemUnderTest(
        experimentId: UUID,
        systemUnderTestInput: SystemUnderTestInput,
    ): LoadTestExperiment {
        val experiment = experimentRepository.findById(experimentId)
            .orElseThrow { IllegalArgumentException("LoadTestExperiment with id $experimentId not found") }

        val systemUnderTest = SystemUnderTest(
            experimentId = experiment.id,
            name = systemUnderTestInput.name,
            composeFileId = systemUnderTestInput.composeFileId,
            jarFileId = systemUnderTestInput.jarFileId,
            description = systemUnderTestInput.description,
            healthCheckPath = systemUnderTestInput.healthCheckPath,
            appPort = systemUnderTestInput.appPort,
            startupTimeoutSeconds = systemUnderTestInput.startupTimeoutSeconds,
        )
        experiment.addSystemUnderTest(systemUnderTest)

        return experimentRepository.save(experiment)
    }

    fun updateSystemUnderTest(
        experimentId: UUID,
        sutId: UUID,
        systemUnderTestInput: SystemUnderTestInput,
    ): SystemUnderTest {
        val sut = systemUnderTestRepository.findById(sutId)
            .orElseThrow { IllegalArgumentException("SystemUnderTest with id $sutId not found") }

        require(sut.experimentId == experimentId) {
            "SystemUnderTest with id $sutId does not belong to experiment $experimentId"
        }

        sut.name = systemUnderTestInput.name
        sut.composeFileId = systemUnderTestInput.composeFileId
        sut.jarFileId = systemUnderTestInput.jarFileId
        sut.description = systemUnderTestInput.description
        sut.healthCheckPath = systemUnderTestInput.healthCheckPath
        sut.appPort = systemUnderTestInput.appPort
        sut.startupTimeoutSeconds = systemUnderTestInput.startupTimeoutSeconds

        return systemUnderTestRepository.save(sut)
    }

    fun  deleteSystemUnderTest(
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

    data class SystemUnderTestInput(
        val name: String,
        val composeFileId: UUID,
        val jarFileId: UUID,
        val description: String? = null,
        val healthCheckPath: String = "/actuator/health",
        val appPort: Int = 9090,
        val startupTimeoutSeconds: Long = 180,
    )
}
