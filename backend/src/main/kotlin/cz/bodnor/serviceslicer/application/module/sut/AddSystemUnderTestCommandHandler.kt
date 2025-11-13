package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddSystemUnderTestCommandHandler(
    private val loadTestExperimentWriteService: LoadTestExperimentWriteService,
) : CommandHandler<AddSystemUnderTestCommand.Result, AddSystemUnderTestCommand> {

    override val command = AddSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: AddSystemUnderTestCommand): AddSystemUnderTestCommand.Result {
        val experiment = loadTestExperimentWriteService.addSystemUnderTest(
            experimentId = command.experimentId,
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

        return AddSystemUnderTestCommand.Result(
            systemUnderTestId = experiment.systemsUnderTest.last().id,
        )
    }
}
