package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UpdateSystemUnderTestCommandHandler(
    private val loadTestExperimentWriteService: LoadTestExperimentWriteService,
) : CommandHandler<UpdateSystemUnderTestCommand.Result, UpdateSystemUnderTestCommand> {

    override val command = UpdateSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: UpdateSystemUnderTestCommand): UpdateSystemUnderTestCommand.Result {
        val sut = loadTestExperimentWriteService.updateSystemUnderTest(
            experimentId = command.experimentId,
            sutId = command.sutId,
            systemUnderTestInput = LoadTestExperimentWriteService.SystemUnderTestInput(
                name = command.name,
                composeFileId = command.composeFileId,
                jarFileId = command.jarFileId,
                description = command.description,
                healthCheckPath = command.healthCheckPath,
                appPort = command.appPort,
                startupTimeoutSeconds = command.startupTimeoutSeconds,
            ),
        )

        return UpdateSystemUnderTestCommand.Result(
            systemUnderTestId = sut.id,
        )
    }
}
