package cz.bodnor.serviceslicer.domain.loadtestexperiment

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoadTestExperimentWriteService(
    private val experimentRepository: LoadTestExperimentRepository,
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
