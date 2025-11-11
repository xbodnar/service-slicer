package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class LoadTestExperimentReadService(
    private val repository: LoadTestExperimentRepository,
) : BaseFinderService<LoadTestExperiment>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = LoadTestExperiment::class
}
