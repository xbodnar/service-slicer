package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class AddSystemUnderTestCommandHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val fileReadService: FileReadService,
) : CommandHandler<AddSystemUnderTestCommand.Result, AddSystemUnderTestCommand> {

    override val command = AddSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: AddSystemUnderTestCommand): AddSystemUnderTestCommand.Result {
        validateFileExists(command.dockerConfig.composeFileId)
        command.databaseSeedConfigs.forEach { config ->
            validateFileExists(config.sqlSeedFileId)
        }

        val benchmark = benchmarkReadService.getById(command.benchmarkId)
        val newSystemUnderTest = SystemUnderTest(
            benchmarkId = command.benchmarkId,
            name = command.name,
            description = command.description,
            isBaseline = command.isBaseline,
            dockerConfig = command.dockerConfig,
            databaseSeedConfigs = command.databaseSeedConfigs,
        )

        benchmark.addSystemUnderTest(newSystemUnderTest)

        return AddSystemUnderTestCommand.Result(
            systemUnderTestId = newSystemUnderTest.id,
        )
    }

    private fun validateFileExists(zipFileId: UUID) {
        val file = fileReadService.getById(zipFileId)
        require(file.status == FileStatus.READY) { "File is not uploaded yet" }
    }
}
