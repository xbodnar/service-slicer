package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.file.command.FetchGitRepositoryCommand
import cz.bodnor.serviceslicer.application.module.file.command.InitiateFileUploadCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request to initiate a file upload")
data class InitiateFileUploadRequest(
    @field:NotBlank
    @Schema(description = "Name of the file to upload", example = "application.jar")
    val filename: String,
    @field:Min(1)
    @Schema(description = "Size of the file in bytes", example = "1048576")
    val size: Long,
    @field:NotBlank
    @Schema(description = "MIME type of the file", example = "application/java-archive")
    val mimeType: String,
) {
    fun toCommand(): InitiateFileUploadCommand = InitiateFileUploadCommand(
        filename = filename,
        size = size,
        mimeType = mimeType,
    )
}

@Schema(description = "Request to fetch a Git repository")
data class FetchGitRepositoryRequest(
    @Schema(description = "URL of the Git repository", example = "https://github.com/user/repo.git")
    val repositoryUrl: String,
    @Schema(description = "Branch to fetch", example = "main")
    val branch: String,
) {
    fun toCommand(): FetchGitRepositoryCommand = FetchGitRepositoryCommand(
        repositoryUrl = repositoryUrl,
        branch = branch,
    )
}
