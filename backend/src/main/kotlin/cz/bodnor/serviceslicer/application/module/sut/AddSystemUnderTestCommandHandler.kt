package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.loadtestexperiment.LoadTestExperimentReadService
import cz.bodnor.serviceslicer.domain.loadtestexperiment.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class AddSystemUnderTestCommandHandler(
    private val loadTestExperimentReadService: LoadTestExperimentReadService,
    private val fileReadService: FileReadService,
) : CommandHandler<AddSystemUnderTestCommand.Result, AddSystemUnderTestCommand> {

    override val command = AddSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: AddSystemUnderTestCommand): AddSystemUnderTestCommand.Result {
        validateFileExists(command.dockerConfig.composeFileId)
        command.databaseSeedConfig?.let {
            validateFileExists(it.sqlSeedFileId)
        }

        val loadTestExperiment = loadTestExperimentReadService.getById(command.experimentId)
        val newSystemUnderTest = SystemUnderTest(
            experimentId = loadTestExperiment.id,
            name = command.name,
            description = command.description,
            isBaseline = command.isBaseline,
            dockerConfig = command.dockerConfig,
            databaseSeedConfig = command.databaseSeedConfig,
        )

        loadTestExperiment.addSystemUnderTest(newSystemUnderTest)

        return AddSystemUnderTestCommand.Result(
            systemUnderTestId = newSystemUnderTest.id,
        )
    }

    private fun validateFileExists(zipFileId: UUID) {
        val file = fileReadService.getById(zipFileId)
        require(file.status == FileStatus.READY) { "File is not uploaded yet" }
    }
}
