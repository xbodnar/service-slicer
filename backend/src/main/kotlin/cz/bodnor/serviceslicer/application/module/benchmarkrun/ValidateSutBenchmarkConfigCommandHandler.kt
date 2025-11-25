package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ValidateSutBenchmarkConfigCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.event.ValidateSutBenchmarkEvent
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ValidateSutBenchmarkConfigCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkWriteService: BenchmarkWriteService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<ValidationResult, ValidateSutBenchmarkConfigCommand> {

    override val command = ValidateSutBenchmarkConfigCommand::class

    @Transactional
    override fun handle(command: ValidateSutBenchmarkConfigCommand): ValidationResult {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        val sut = benchmark.getSystemUnderTest(command.systemUnderTestId)
        val pendingValidationResult = ValidationResult()

        sut.validationResult = pendingValidationResult
        benchmarkWriteService.save(benchmark)

        applicationEventPublisher.publishEvent(
            ValidateSutBenchmarkEvent(benchmarkId = command.benchmarkId, systemUnderTestId = command.systemUnderTestId),
        )

        return pendingValidationResult
    }
}
