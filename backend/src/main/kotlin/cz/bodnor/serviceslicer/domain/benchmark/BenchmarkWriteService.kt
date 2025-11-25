package cz.bodnor.serviceslicer.domain.benchmark

import org.springframework.stereotype.Service

@Service
class BenchmarkWriteService(
    private val benchmarkRepository: BenchmarkRepository,
) {

    fun create(
        benchmarkConfig: BenchmarkConfig,
        name: String,
        description: String? = null,
        systemsUnderTest: List<SystemUnderTest>,
    ): Benchmark {
        val benchmark = Benchmark(
            config = benchmarkConfig,
            name = name,
            description = description,
            systemsUnderTest = systemsUnderTest.toMutableList(),
        )

        return benchmarkRepository.save(benchmark)
    }

    fun save(benchmark: Benchmark) = benchmarkRepository.save(benchmark)
}
