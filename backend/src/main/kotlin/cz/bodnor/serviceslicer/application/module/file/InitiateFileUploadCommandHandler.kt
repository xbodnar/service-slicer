package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.command.InitiateFileUploadCommand
import cz.bodnor.serviceslicer.application.module.file.port.out.GenerateFileUploadUrl
import cz.bodnor.serviceslicer.domain.file.FileWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class InitiateFileUploadCommandHandler(
    private val fileWriteService: FileWriteService,
    private val generateFileUploadUrl: GenerateFileUploadUrl,
) : CommandHandler<InitiateFileUploadCommand.Result, InitiateFileUploadCommand> {

    override val command = InitiateFileUploadCommand::class

    @Transactional
    override fun handle(command: InitiateFileUploadCommand): InitiateFileUploadCommand.Result {
        val file = fileWriteService.create(
            filename = command.filename,
            mimeType = command.mimeType,
            expectedSize = command.size,
        )

        val uploadUrl = generateFileUploadUrl(file.storageKey)

        return InitiateFileUploadCommand.Result(
            fileId = file.id,
            uploadUrl = uploadUrl,
        )
    }
}
