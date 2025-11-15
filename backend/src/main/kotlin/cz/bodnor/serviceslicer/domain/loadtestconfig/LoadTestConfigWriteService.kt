package cz.bodnor.serviceslicer.domain.loadtestconfig

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoadTestConfigWriteService(
    private val repository: LoadTestConfigurationRepository,
) {

    fun create(
        openApiFileId: UUID,
        behaviorModels: List<BehaviorModel>,
        operationalProfile: List<OperationalLoad>,
        k6Configuration: K6Configuration? = null,
    ): LoadTestConfig = repository.save(
        LoadTestConfig(
            id = UUID.randomUUID(),
            openApiFileId = openApiFileId,
            behaviorModels = behaviorModels,
            operationalProfile = operationalProfile,
            k6Configuration = k6Configuration,
        ),
    )
}
