package cz.bodnor.serviceslicer.adapter.`in`.event

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RunSutValidationCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.event.ValidateSutBenchmarkEvent
import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.application.module.job.JobLauncherService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWriteService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class BenchmarkRunEventListener(
    private val commandBus: CommandBus,
) {

    private val logger = logger()

    // TODO: Reimplement as Spring Batch Job + Scheduler so validations don't interfere with benchmarkRuns
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onValidateSutBenchmarkEvent(event: ValidateSutBenchmarkEvent) {
        commandBus(
            RunSutValidationCommand(
                benchmarkId = event.benchmarkId,
                systemUnderTestId = event.systemUnderTestId,
            ),
        )
    }
}
