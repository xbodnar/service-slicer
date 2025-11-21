package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.applicationError
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateSystemUnderTestCommandHandler(
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
) : CommandHandler<UpdateSystemUnderTestCommand.Result, UpdateSystemUnderTestCommand> {

    override val command = UpdateSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: UpdateSystemUnderTestCommand): UpdateSystemUnderTestCommand.Result {
        val loadTestExperiment = loadTestExperimentReadService.getById(command.experimentId)
        val sut = loadTestExperimentReadService.getSystemUnderTestById(command.experimentId, command.sutId)

        sut.name = command.name
        sut.description = command.description
        sut.dockerConfig = command.dockerConfig
        sut.databaseSeedConfig = command.databaseSeedConfig

        return UpdateSystemUnderTestCommand.Result(
            systemUnderTestId = sut.id,
        )
    }
}
