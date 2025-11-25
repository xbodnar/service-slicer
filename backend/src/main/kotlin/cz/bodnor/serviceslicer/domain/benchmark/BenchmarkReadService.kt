package cz.bodnor.serviceslicer.domain.benchmark

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class BenchmarkReadService(
    private val repository: BenchmarkRepository,
) : BaseFinderService<Benchmark>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = Benchmark::class
}
