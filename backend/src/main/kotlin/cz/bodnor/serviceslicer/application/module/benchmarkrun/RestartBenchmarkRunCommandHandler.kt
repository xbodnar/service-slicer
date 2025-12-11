package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.RestartBenchmarkRunCommand
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RestartBenchmarkRunCommandHandler(
    private val benchmarkRunReadService: BenchmarkRunReadService,
) : CommandHandler<BenchmarkRun, RestartBenchmarkRunCommand> {

    override val command = RestartBenchmarkRunCommand::class

    @Transactional
    override fun handle(command: RestartBenchmarkRunCommand): BenchmarkRun {
        val benchmarkRun = benchmarkRunReadService.getById(command.benchmarkRunId)
        verify(benchmarkRun.status == JobStatus.FAILED) {
            "Benchmark run is not failed, therefore cannot be restarted"
        }

        benchmarkRun.queue()

        return benchmarkRun
    }
}
