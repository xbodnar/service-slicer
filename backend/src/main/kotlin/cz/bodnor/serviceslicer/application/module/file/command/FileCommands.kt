package cz.bodnor.serviceslicer.application.module.file.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class InitiateFileUploadCommand(
    val filename: String,
    val size: Long,
    val mimeType: String,
) : Command<InitiateFileUploadCommand.Result> {
    @Schema(name = "InitiateFileUploadResult", description = "Result of initiating a file upload")
    data class Result(
        @Schema(description = "ID of the created file record")
        val fileId: UUID,
        @Schema(description = "Presigned URL for uploading the file to object storage")
        val uploadUrl: String,
        @Schema(description = "Storage key for the file in object storage")
        val storageKey: String,
    )
}

data class CompleteFileUploadCommand(
    val fileId: UUID,
) : Command<Unit>

data class ExtractZipFileCommand(
    val zipFileId: UUID,
) : Command<ExtractZipFileCommand.Result> {

    @Schema(name = "ExtractZipFileResult", description = "Result of extracting a ZIP file")
    data class Result(
        @Schema(description = "ID of the created directory file record")
        val dirId: UUID,
    )
}

data class FetchGitRepositoryCommand(
    val repositoryUrl: String,
    val branch: String,
) : Command<FetchGitRepositoryCommand.Result> {

    @Schema(name = "FetchGitRepositoryResult", description = "Result of fetching a Git repository")
    data class Result(
        @Schema(description = "ID of the created directory file record")
        val dirId: UUID,
    )
}
