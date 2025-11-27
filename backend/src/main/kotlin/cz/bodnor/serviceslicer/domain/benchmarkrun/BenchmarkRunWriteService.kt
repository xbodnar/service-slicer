package cz.bodnor.serviceslicer.domain.benchmarkrun

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BenchmarkRunWriteService(
    private val repository: BenchmarkRunRepository,
) {

    fun create(benchmarkId: UUID): BenchmarkRun = repository.save(BenchmarkRun(benchmarkId = benchmarkId))

    fun save(benchmarkRun: BenchmarkRun): BenchmarkRun = repository.save(benchmarkRun)
}
