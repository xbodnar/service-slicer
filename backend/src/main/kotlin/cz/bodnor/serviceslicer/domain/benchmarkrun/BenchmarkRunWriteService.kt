package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCase
import org.springframework.stereotype.Service
import kotlin.time.Duration

@Service
class BenchmarkRunWriteService(
    private val repository: BenchmarkRunRepository,
) {

    fun create(
        benchmark: Benchmark,
        testDuration: Duration,
    ): BenchmarkRun {
        val baselineTestCase =
            BaselineTestCase(
                baselineSut = benchmark.baselineSut,
                operationalProfile = benchmark.operationalSetting.operationalProfile,
            )

        val benchmarkRun = BenchmarkRun(
            benchmark = benchmark,
            baselineTestCase = baselineTestCase,
            testDuration = testDuration,
        )

        benchmark.operationalSetting.operationalProfile.forEach { profile ->
            benchmarkRun.addTargetTestCase(
                sut = benchmark.targetSut,
                load = profile.key,
                frequency = profile.value,
            )
        }

        return repository.save(benchmarkRun)
    }

    fun save(benchmarkRun: BenchmarkRun): BenchmarkRun = repository.save(benchmarkRun)
}
