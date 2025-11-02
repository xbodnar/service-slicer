package cz.bodnor.serviceslicer.adapter.`in`.web.requests

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
