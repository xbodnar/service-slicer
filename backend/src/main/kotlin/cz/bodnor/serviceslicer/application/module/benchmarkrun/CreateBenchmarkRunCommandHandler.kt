package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmark.event.BenchmarkRunCreatedEvent
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.CreateBenchmarkRunCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateBenchmarkRunCommandHandler(
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val benchmarkReadService: BenchmarkReadService,
    private val sutReadService: SystemUnderTestReadService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : CommandHandler<CreateBenchmarkRunCommand.Result, CreateBenchmarkRunCommand> {

    override val command = CreateBenchmarkRunCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkRunCommand): CreateBenchmarkRunCommand.Result {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        val benchmarkRun = benchmarkRunWriteService.create(benchmark)

        applicationEventPublisher.publishEvent(BenchmarkRunCreatedEvent(benchmarkRunId = benchmarkRun.id))

        return CreateBenchmarkRunCommand.Result(benchmarkRun.id)
    }
}
