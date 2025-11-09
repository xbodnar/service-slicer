package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateLoadTestExperimentCommandHandler(
    private val loadTestExperimentWriteService: LoadTestExperimentWriteService,
    private val loadTestConfigReadService: LoadTestConfigReadService,
) : CommandHandler<CreateLoadTestExperimentCommand.Result, CreateLoadTestExperimentCommand> {

    override val command = CreateLoadTestExperimentCommand::class

    @Autowired
    @Lazy
    private lateinit var commandBus: CommandBus

    @Transactional
    override fun handle(command: CreateLoadTestExperimentCommand): CreateLoadTestExperimentCommand.Result {
        val loadTestConfig = commandBus(command.loadTestConfig.toCommand())

        // TODO: Check that app port is on 8080
        // TODO: Setup fixed resources for the SUTs

        val experiment = loadTestExperimentWriteService.create(
            loadTestConfigId = loadTestConfig.loadTestConfigId,
            name = command.name,
            description = command.description,
            systemsUnderTest = command.systemsUnderTest.map { sut ->
                LoadTestExperimentWriteService.SystemUnderTestInput(
                    name = sut.name,
                    composeFileId = sut.composeFileId,
                    jarFileId = sut.jarFileId,
                    description = sut.description,
                    healthCheckPath = sut.healthCheckPath,
                    appPort = sut.appPort,
                    startupTimeoutSeconds = sut.startupTimeoutSeconds,
                )
            },
        )

        return CreateLoadTestExperimentCommand.Result(experimentId = experiment.id)
    }
}
