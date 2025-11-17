package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AddSystemUnderTestCommandHandler(
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
) : CommandHandler<AddSystemUnderTestCommand.Result, AddSystemUnderTestCommand> {

    override val command = AddSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: AddSystemUnderTestCommand): AddSystemUnderTestCommand.Result {
        val loadTestExperiment = loadTestExperimentReadService.getById(command.experimentId)
        val newSystemUnderTest = SystemUnderTest(
            experimentId = loadTestExperiment.id,
            name = command.name,
            composeFileId = command.composeFileId,
            jarFileId = command.jarFileId,
            sqlSeedFileId = command.sqlSeedFileId,
            description = command.description,
            healthCheckPath = command.healthCheckPath,
            appPort = command.appPort,
            startupTimeoutSeconds = command.startupTimeoutSeconds,
        )

        loadTestExperiment.addSystemUnderTest(newSystemUnderTest)

        return AddSystemUnderTestCommand.Result(
            systemUnderTestId = newSystemUnderTest.id,
        )
    }
}
