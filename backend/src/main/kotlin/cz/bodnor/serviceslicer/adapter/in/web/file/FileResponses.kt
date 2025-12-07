package cz.bodnor.serviceslicer.adapter.`in`.web.file

import cz.bodnor.serviceslicer.domain.file.FileStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "List of files")
data class ListFilesResponse(
    val items: List<FileDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

@Schema(description = "File DTO")
data class FileDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val filename: String,
    val fileSize: Long,
    val mimeType: String,
    val status: FileStatus,
)

@Schema(description = "Result of initiating a file upload")
data class InitiateFileUploadResponse(
    @Schema(description = "ID of the created file record")
    val fileId: UUID,
    @Schema(description = "Presigned URL for uploading the file to object storage")
    val uploadUrl: String,
)

@Schema(description = "Presigned download URL for a file")
data class GetDownloadUrlResponse(
    val downloadUrl: String,
)
