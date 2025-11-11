package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.file.command.FetchGitRepositoryCommand
import cz.bodnor.serviceslicer.application.module.file.command.InitiateFileUploadCommand
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class InitiateFileUploadRequest(
    @field:NotBlank
    val filename: String,
    @field:Min(1)
    val size: Long,
    @field:NotBlank
    val mimeType: String,
    @field:NotBlank
    val contentHash: String,
) {
    fun toCommand(): InitiateFileUploadCommand = InitiateFileUploadCommand(
        filename = filename,
        size = size,
        mimeType = mimeType,
        contentHash = contentHash,
    )
}

data class FetchGitRepositoryRequest(
    val repositoryUrl: String,
    val branch: String,
) {
    fun toCommand(): FetchGitRepositoryCommand = FetchGitRepositoryCommand(
        repositoryUrl = repositoryUrl,
        branch = branch,
    )
}
