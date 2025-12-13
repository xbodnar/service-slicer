package cz.bodnor.serviceslicer.domain.benchmarkrun

import org.springframework.stereotype.Service

@Service
class BenchmarkRunWriteService(
    private val repository: BenchmarkRunRepository,
) {

    fun save(benchmarkRun: BenchmarkRun): BenchmarkRun = repository.save(benchmarkRun)
}
