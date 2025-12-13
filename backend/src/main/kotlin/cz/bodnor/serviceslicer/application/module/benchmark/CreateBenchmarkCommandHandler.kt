package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkCommand
import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateBenchmarkCommandHandler(
    private val benchmarkWriteService: BenchmarkWriteService,
    private val operationalSettingReadService: OperationalSettingReadService,
    private val sutReadService: SystemUnderTestReadService,
) : CommandHandler<Benchmark, CreateBenchmarkCommand> {

    override val command = CreateBenchmarkCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkCommand): Benchmark {
        verify(command.systemsUnderTest.contains(command.baselineSutId)) {
            "Baseline SUT is not part of the systems under test"
        }
        val systemsUnderTest = sutReadService.findAllByIds(command.systemsUnderTest.toSet())
        verify(systemsUnderTest.size == command.systemsUnderTest.size) {
            "Failed to load all systems under test, missing: " +
                "${command.systemsUnderTest - systemsUnderTest.map { it.id }.toSet()}"
        }

        val operationalSetting = operationalSettingReadService.getById(command.operationalSettingId)

        val benchmark = Benchmark(
            name = command.name,
            description = command.description,
            operationalSetting = operationalSetting,
        )

        systemsUnderTest.forEach { benchmark.addSystemUnderTest(it, it.id == command.baselineSutId) }

        return benchmarkWriteService.save(benchmark)
    }
}
