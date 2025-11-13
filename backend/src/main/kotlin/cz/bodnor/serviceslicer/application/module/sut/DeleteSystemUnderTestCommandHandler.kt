package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteSystemUnderTestCommandHandler(
    private val loadTestExperimentWriteService: LoadTestExperimentWriteService,
) : CommandHandler<Unit, DeleteSystemUnderTestCommand> {

    override val command = DeleteSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: DeleteSystemUnderTestCommand) {
        loadTestExperimentWriteService.deleteSystemUnderTest(
            experimentId = command.experimentId,
            sutId = command.sutId,
        )
    }
}
