package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.ExecuteBenchmarkValidationCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.K6Runner
import cz.bodnor.serviceslicer.application.module.benchmarkrun.service.SystemUnderTestRunner
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.benchmarkvalidation.BenchmarkSutValidationRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkvalidation.BenchmarkSutValidationRunWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class ExecuteBenchmarkValidationCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkWriteService: BenchmarkWriteService,
    private val benchmarkValidationRunReadService: BenchmarkSutValidationRunReadService,
    private val benchmarkValidationRunWriteService: BenchmarkSutValidationRunWriteService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
) : CommandHandler<Unit, ExecuteBenchmarkValidationCommand> {

    override val command = ExecuteBenchmarkValidationCommand::class

    private val logger = KotlinLogging.logger {}

    @Lazy
    @Autowired
    private lateinit var self: ExecuteBenchmarkValidationCommandHandler

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    override fun handle(command: ExecuteBenchmarkValidationCommand) {
        val validationRun = benchmarkValidationRunReadService.getById(command.benchmarkValidationRunId)

        val benchmark = validationRun.benchmarkSystemUnderTest.benchmark
        val sut = validationRun.benchmarkSystemUnderTest.systemUnderTest

        logger.info {
            "Executing benchmark validation run: ${validationRun.id}, benchmark: ${benchmark.id}, SUT: ${sut.id}"
        }

        self.beforeValidationRun(command.benchmarkValidationRunId)

        try {
            // Start the SUT (blocking call - waits until SUT is healthy and ready)
            sutRunner.startSUT(sut)

            // Run validation test (no metrics, single iteration through all behavior models)
            val k6Result = k6Runner.runValidation(
                operationalSettingId = benchmark.operationalSetting.id,
                appPort = sut.dockerConfig.appPort,
            )

            self.validationRunCompleted(validationRun.id, k6Result.output)
        } catch (e: Exception) {
            logger.error(e) { "Validation run ${validationRun.id} failed, benchmark: ${benchmark.id}, SUT: ${sut.id}" }
            self.validationRunFailed(validationRun.id, e.message ?: "Unknown error during validation")
        } finally {
            logger.info { "Validation run finished, cleaning up..." }
            sutRunner.stopSUT()
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun beforeValidationRun(benchmarkValidationRunId: UUID) {
        val validationRun = benchmarkValidationRunReadService.getById(benchmarkValidationRunId)
        validationRun.started()
        benchmarkValidationRunWriteService.save(validationRun)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun validationRunCompleted(
        benchmarkValidationRunId: UUID,
        k6Output: String,
    ) {
        val validationRun = benchmarkValidationRunReadService.getById(benchmarkValidationRunId)
        validationRun.completed(k6Output)
        benchmarkValidationRunWriteService.save(validationRun)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun validationRunFailed(
        benchmarkValidationRunId: UUID,
        errorMessage: String,
    ) {
        val validationRun = benchmarkValidationRunReadService.getById(benchmarkValidationRunId)
        validationRun.failed(errorMessage)
        benchmarkValidationRunWriteService.save(validationRun)
    }
}
