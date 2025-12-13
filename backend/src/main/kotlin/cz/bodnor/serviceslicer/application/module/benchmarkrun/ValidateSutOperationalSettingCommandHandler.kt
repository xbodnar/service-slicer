package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ValidateSutOperationalSettingCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.event.ValidateSutBenchmarkEvent
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class ValidateSutOperationalSettingCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkWriteService: BenchmarkWriteService,
    private val sutReadService: SystemUnderTestReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<ValidationResult, ValidateSutOperationalSettingCommand> {

    override val command = ValidateSutOperationalSettingCommand::class

    @Transactional
    override fun handle(command: ValidateSutOperationalSettingCommand): ValidationResult {
        val sut = sutReadService.getById(command.systemUnderTestId)
        val benchmark = benchmarkReadService.getById(command.benchmarkId)

        TODO()
//        benchmark.startValidationRun(sut.id)
        benchmarkWriteService.save(benchmark)

        applicationEventPublisher.publishEvent(
            ValidateSutBenchmarkEvent(benchmarkId = command.benchmarkId, systemUnderTestId = command.systemUnderTestId),
        )

        return ValidationResult()
    }
}
