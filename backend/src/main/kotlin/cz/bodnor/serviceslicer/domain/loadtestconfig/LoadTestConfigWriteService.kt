package cz.bodnor.serviceslicer.domain.loadtestconfig

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LoadTestConfigWriteService(
    private val repository: LoadTestConfigurationRepository,
) {

    fun create(loadTestConfig: LoadTestConfig): LoadTestConfig = repository.save(loadTestConfig)
}
