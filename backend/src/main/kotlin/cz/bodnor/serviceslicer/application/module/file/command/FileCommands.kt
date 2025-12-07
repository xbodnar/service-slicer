package cz.bodnor.serviceslicer.application.module.file.command

import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class InitiateFileUploadCommand(
    val filename: String,
    val size: Long,
    val mimeType: String,
) : Command<InitiateFileUploadCommand.Result> {
    data class Result(
        val fileId: UUID,
        val uploadUrl: String,
    )
}

data class CompleteFileUploadCommand(
    val fileId: UUID,
) : Command<File>
