package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateBenchmarkCommandHandler(
    private val benchmarkWriteService: BenchmarkWriteService,
    private val fileReadService: FileReadService,
) : CommandHandler<CreateBenchmarkCommand.Result, CreateBenchmarkCommand> {

    override val command = CreateBenchmarkCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkCommand): CreateBenchmarkCommand.Result {
        val benchmark = benchmarkWriteService.create(
            benchmarkConfig = command.benchmarkConfig,
            name = command.name,
            description = command.description,
        )
        command.systemsUnderTest.forEach { benchmark.addSystemUnderTest(it.toDomain(benchmark.id)) }

        return CreateBenchmarkCommand.Result(benchmarkId = benchmark.id)
    }
}
