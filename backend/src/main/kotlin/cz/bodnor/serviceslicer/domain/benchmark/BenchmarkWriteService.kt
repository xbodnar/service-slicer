package cz.bodnor.serviceslicer.domain.benchmark

import org.springframework.stereotype.Service

@Service
class BenchmarkWriteService(
    private val benchmarkRepository: BenchmarkRepository,
) {

    fun save(benchmark: Benchmark) = benchmarkRepository.save(benchmark)
}
