package cz.bodnor.serviceslicer.domain.loadtestconfig

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoadTestConfigWriteService(
    private val repository: LoadTestConfigurationRepository,
) {

    fun create(
        openApiFileId: UUID,
        name: String,
        behaviorModels: List<BehaviorModel> = emptyList(),
        operationalProfile: OperationalProfile? = null,
        k6Configuration: K6Configuration? = null,
    ): LoadTestConfig = repository.save(
        LoadTestConfig(
            id = UUID.randomUUID(),
            openApiFileId = openApiFileId,
            name = name,
            behaviorModels = behaviorModels,
            operationalProfile = operationalProfile,
            k6Configuration = k6Configuration,
        ),
    )
}
