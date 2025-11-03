package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.command.CompleteFileUploadCommand
import cz.bodnor.serviceslicer.application.module.file.port.out.GetFileMetadataFromStorage
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class CompleteFileUploadCommandHandler(
    private val fileReadService: FileReadService,
    private val getFileMetadataFromStorage: GetFileMetadataFromStorage,
) : CommandHandler<Unit, CompleteFileUploadCommand> {

    override val command = CompleteFileUploadCommand::class

    @Transactional
    override fun handle(command: CompleteFileUploadCommand) {
        val file = fileReadService.getById(command.fileId)

        // Verify file exists in storage
        val fileMetadata = getFileMetadataFromStorage(file.storageKey)

        // Verify size matches
        require(fileMetadata.size == file.expectedSize) {
            file.markAsFailed()
            "File size does not match"
        }

        file.markAsReady()
    }
}
