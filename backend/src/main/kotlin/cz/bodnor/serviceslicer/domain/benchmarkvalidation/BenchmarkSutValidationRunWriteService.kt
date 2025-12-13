package cz.bodnor.serviceslicer.domain.benchmarkvalidation

import org.springframework.stereotype.Service

@Service
class BenchmarkSutValidationRunWriteService(
    private val repository: BenchmarkSutValidationRunRepository,
) {

    fun save(validationRun: BenchmarkSutValidationRun) = repository.save(validationRun)
}
