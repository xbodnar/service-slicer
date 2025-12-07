package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmark.event.BenchmarkRunCreatedEvent
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.CreateBenchmarkRunCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Duration

@Component
class CreateBenchmarkRunCommandHandler(
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
    private val benchmarkReadService: BenchmarkReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val k6Properties: K6Properties,
) : CommandHandler<BenchmarkRun, CreateBenchmarkRunCommand> {

    override val command = CreateBenchmarkRunCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkRunCommand): BenchmarkRun {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        val benchmarkRun = benchmarkRunWriteService.create(
            benchmark = benchmark,
            testDuration = Duration.parse(command.testDuration ?: k6Properties.testDuration),
        )

        applicationEventPublisher.publishEvent(BenchmarkRunCreatedEvent(benchmarkRunId = benchmarkRun.id))

        return benchmarkRun
    }
}
