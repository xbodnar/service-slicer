package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkWriteService
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingReadService
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingWriteService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateBenchmarkCommandHandler(
    private val benchmarkWriteService: BenchmarkWriteService,
    private val operationalSettingReadService: OperationalSettingReadService,
    private val operationalSettingWriteService: OperationalSettingWriteService,
    private val sutReadService: SystemUnderTestReadService,
) : CommandHandler<CreateBenchmarkCommand.Result, CreateBenchmarkCommand> {

    override val command = CreateBenchmarkCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkCommand): CreateBenchmarkCommand.Result {
        verify(command.baselineSutId != command.targetSutId) {
            "Baseline and target SUT must be different"
        }
        val baselineSut = sutReadService.getById(command.baselineSutId)
        val targetSut = sutReadService.getById(command.targetSutId)

        if (!operationalSettingReadService.existsById(command.operationalSetting.id)) {
            operationalSettingWriteService.save(command.operationalSetting)
        }

        val benchmark = benchmarkWriteService.create(
            operationalSetting = command.operationalSetting,
            name = command.name,
            description = command.description,
            baselineSut = baselineSut,
            targetSut = targetSut,
        )

        return CreateBenchmarkCommand.Result(benchmarkId = benchmark.id)
    }
}
