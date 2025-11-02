package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.command.InitiateFileUploadCommand
import cz.bodnor.serviceslicer.application.module.file.port.out.GenerateFileUploadUrl
import cz.bodnor.serviceslicer.domain.file.FileRepository
import cz.bodnor.serviceslicer.domain.file.FileWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InitiateFileUploadCommandHandler(
    private val fileWriteService: FileWriteService,
    private val generateFileUploadUrl: GenerateFileUploadUrl,
) : CommandHandler<InitiateFileUploadCommand.Result, InitiateFileUploadCommand> {

    override val command = InitiateFileUploadCommand::class

    override fun handle(command: InitiateFileUploadCommand): InitiateFileUploadCommand.Result {
        val storageKey = generateStorageKey(command.filename)

        val file = fileWriteService.create(
            filename = command.filename,
            mimeType = command.mimeType,
            expectedSize = command.size,
            contentHash = command.contentHash,
            storageKey = storageKey,
        )

        val uploadUrl = generateFileUploadUrl(storageKey)

        return InitiateFileUploadCommand.Result(
            fileId = file.id,
            uploadUrl = uploadUrl,
            storageKey = storageKey,
        )
    }

    private fun generateStorageKey(filename: String): String {
        val uuid = UUID.randomUUID().toString()
        val sanitizedFilename = filename.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return "$uuid/$sanitizedFilename"
    }
}
