package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class BenchmarkRunReadService(
    private val repository: BenchmarkRunRepository,
) : BaseFinderService<BenchmarkRun>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = BenchmarkRun::class
}
