package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RunSutValidationCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.ValidationRunner
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class RunSutValidationCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkWriteService: BenchmarkWriteService,
    private val validationRunner: ValidationRunner,
) : CommandHandler<Unit, RunSutValidationCommand> {

    override val command = RunSutValidationCommand::class

    private val logger = KotlinLogging.logger {}

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun handle(command: RunSutValidationCommand) {
        logger.info { "Validating SUT ${command.systemUnderTestId} for benchmark ${command.benchmarkId}" }

        val result = validationRunner.runSutValidation(
            benchmarkId = command.benchmarkId,
            systemUnderTestId = command.systemUnderTestId,
        )

        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        benchmark.completeValidationRun(command.systemUnderTestId, result)

        benchmarkWriteService.save(benchmark)

        logger.info {
            "Validation completed for SUT ${command.systemUnderTestId} with state: ${result.validationState}"
        }
    }
}
