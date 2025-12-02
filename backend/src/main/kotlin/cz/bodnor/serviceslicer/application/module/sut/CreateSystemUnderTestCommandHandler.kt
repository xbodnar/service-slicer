package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.CreateSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class CreateSystemUnderTestCommandHandler(
    private val fileReadService: FileReadService,
    private val sutWriteService: SystemUnderTestWriteService,
) : CommandHandler<CreateSystemUnderTestCommand.Result, CreateSystemUnderTestCommand> {

    override val command = CreateSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: CreateSystemUnderTestCommand): CreateSystemUnderTestCommand.Result {
        validateFileExists(command.dockerConfig.composeFileId)

        command.databaseSeedConfigs.forEach { config ->
            validateFileExists(config.sqlSeedFileId)
        }

        val sut = sutWriteService.save(
            SystemUnderTest(
                name = command.name,
                description = command.description,
                dockerConfig = command.dockerConfig,
                databaseSeedConfigs = command.databaseSeedConfigs,
            ),
        )

        return CreateSystemUnderTestCommand.Result(
            systemUnderTestId = sut.id,
        )
    }

    private fun validateFileExists(zipFileId: UUID) {
        val file = fileReadService.getById(zipFileId)
        require(file.status == FileStatus.READY) { "File is not uploaded yet" }
    }
}
