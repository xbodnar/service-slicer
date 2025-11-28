package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.UpdateBenchmarkCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateBenchmarkCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val benchmarkWriteService: BenchmarkWriteService,
) : CommandHandler<UpdateBenchmarkCommand.Result, UpdateBenchmarkCommand> {

    override val command = UpdateBenchmarkCommand::class

    @Transactional
    override fun handle(command: UpdateBenchmarkCommand): UpdateBenchmarkCommand.Result {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)

        benchmark.name = command.name
        benchmark.description = command.description
        benchmark.config = command.benchmarkConfig

        benchmarkWriteService.save(benchmark)

        return UpdateBenchmarkCommand.Result(benchmarkId = benchmark.id)
    }
}
