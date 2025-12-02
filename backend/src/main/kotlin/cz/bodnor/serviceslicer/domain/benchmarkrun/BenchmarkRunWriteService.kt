package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCase
import org.springframework.stereotype.Service

@Service
class BenchmarkRunWriteService(
    private val repository: BenchmarkRunRepository,
) {

    fun create(benchmark: Benchmark): BenchmarkRun {
        val baselineTestCase =
            BaselineTestCase(
                baselineSut = benchmark.baselineSut,
                operationalProfile = benchmark.config.operationalProfile,
            )

        val benchmarkRun = BenchmarkRun(
            benchmark = benchmark,
            baselineTestCase = baselineTestCase,
        )

        benchmark.config.operationalProfile.forEach { profile ->
            benchmarkRun.addTargetTestCase(
                sut = benchmark.targetSut,
                load = profile,
            )
        }

        return repository.save(benchmarkRun)
    }

    fun save(benchmarkRun: BenchmarkRun): BenchmarkRun = repository.save(benchmarkRun)
}
