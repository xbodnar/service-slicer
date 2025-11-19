package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteSystemUnderTestCommandHandler(
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
) : CommandHandler<Unit, DeleteSystemUnderTestCommand> {

    override val command = DeleteSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: DeleteSystemUnderTestCommand) {
        val loadTestExperiment = loadTestExperimentReadService.getById(command.experimentId)
        loadTestExperiment.removeSystemUnderTest(command.sutId)
    }
}
