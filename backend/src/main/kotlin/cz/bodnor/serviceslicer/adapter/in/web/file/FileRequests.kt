package cz.bodnor.serviceslicer.adapter.`in`.web.file

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
)
