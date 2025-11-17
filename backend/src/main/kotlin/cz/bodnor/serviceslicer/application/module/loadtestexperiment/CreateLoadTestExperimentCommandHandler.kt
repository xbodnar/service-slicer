package cz.bodnor.serviceslicer.application.module.loadtestexperiment

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentWriteService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.neo4j.cypherdsl.core.Cypher.file
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CreateLoadTestExperimentCommandHandler(
    private val loadTestExperimentWriteService: LoadTestExperimentWriteService,
    private val loadTestConfigReadService: LoadTestConfigReadService,
    private val fileReadService: FileReadService,
) : CommandHandler<CreateLoadTestExperimentCommand.Result, CreateLoadTestExperimentCommand> {

    override val command = CreateLoadTestExperimentCommand::class

    @Autowired
    @Lazy
    private lateinit var commandBus: CommandBus

    @Transactional
    override fun handle(command: CreateLoadTestExperimentCommand): CreateLoadTestExperimentCommand.Result {
        val loadTestConfig = commandBus(command.loadTestConfig)

        val experiment = loadTestExperimentWriteService.create(
            loadTestConfigId = loadTestConfig.loadTestConfigId,
            name = command.name,
            description = command.description,
        )

        command.systemsUnderTest.forEach { sut ->
            validateFileExists(sut.jarFileId)
            validateFileExists(sut.composeFileId)
            experiment.addSystemUnderTest(sut.toEntity(experimentId = experiment.id))
        }

        return CreateLoadTestExperimentCommand.Result(experimentId = experiment.id)
    }

    private fun validateFileExists(zipFileId: UUID) {
        val file = fileReadService.getById(zipFileId)
        require(file.status == FileStatus.READY) { "File is not uploaded yet" }
    }

    private fun CreateLoadTestExperimentCommand.CreateSystemUnderTest.toEntity(experimentId: UUID) = SystemUnderTest(
        experimentId = experimentId,
        name = name,
        jarFileId = jarFileId,
        composeFileId = composeFileId,
        sqlSeedFileId = sqlSeedFileId,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}
