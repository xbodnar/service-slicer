package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmark.event.BenchmarkRunRestartedEvent
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RestartBenchmarkRunCommand
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class RestartBenchmarkRunCommandHandler(
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<BenchmarkRun, RestartBenchmarkRunCommand> {

    override val command = RestartBenchmarkRunCommand::class

    override fun handle(command: RestartBenchmarkRunCommand): BenchmarkRun {
        val benchmarkRun = benchmarkRunReadService.getById(command.benchmarkRunId)
        verify(benchmarkRun.status == JobStatus.FAILED) {
            "Benchmark run is not failed, therefore cannot be restarted"
        }

        applicationEventPublisher.publishEvent(BenchmarkRunRestartedEvent(benchmarkRun.id))

        return benchmarkRun
    }
}
