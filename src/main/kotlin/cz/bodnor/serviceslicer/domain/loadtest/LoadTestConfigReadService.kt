package cz.bodnor.serviceslicer.domain.loadtest

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class LoadTestConfigReadService(
    private val repository: LoadTestConfigurationRepository,
) : BaseFinderService<LoadTestConfig>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = LoadTestConfig::class
}
