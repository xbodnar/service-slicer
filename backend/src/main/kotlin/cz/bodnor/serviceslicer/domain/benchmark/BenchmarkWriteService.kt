package cz.bodnor.serviceslicer.domain.benchmark

import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import org.springframework.stereotype.Service

@Service
class BenchmarkWriteService(
    private val benchmarkRepository: BenchmarkRepository,
) {

    fun create(
        operationalSetting: OperationalSetting,
        name: String,
        description: String? = null,
        baselineSut: SystemUnderTest,
        targetSut: SystemUnderTest,
    ): Benchmark {
        val benchmark = Benchmark(
            operationalSetting = operationalSetting,
            name = name,
            description = description,
            baselineSut = baselineSut,
            targetSut = targetSut,
        )

        return benchmarkRepository.save(benchmark)
    }

    fun save(benchmark: Benchmark) = benchmarkRepository.save(benchmark)
}
