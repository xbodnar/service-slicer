package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkValidationRunCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkvalidation.BenchmarkSutValidationRun
import cz.bodnor.serviceslicer.domain.benchmarkvalidation.BenchmarkSutValidationRunWriteService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class CreateBenchmarkValidationRunCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkSutValidationRunWriteService: BenchmarkSutValidationRunWriteService,
) : CommandHandler<BenchmarkSutValidationRun, CreateBenchmarkValidationRunCommand> {

    override val command = CreateBenchmarkValidationRunCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkValidationRunCommand): BenchmarkSutValidationRun {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        val sut = benchmark.systemsUnderTest.find { it.systemUnderTest.id == command.systemUnderTestId }
            ?: throw IllegalArgumentException("SUT not found")

        val validationRun = sut.benchmarkSutValidationRun ?: BenchmarkSutValidationRun(sut)

        if (validationRun.status == JobStatus.RUNNING) {
            throw IllegalArgumentException("Validation already running")
        }

        validationRun.queued()
        benchmarkSutValidationRunWriteService.save(validationRun)

        return validationRun
    }
}
