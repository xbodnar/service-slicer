package cz.bodnor.serviceslicer.application.module.file.query

import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class ListFilesQuery(
    val page: Int = 0,
    val size: Int = 20,
) : Query<ListFilesQuery.Result> {

    @Schema(name = "ListFilesResult", description = "Paginated list of files")
    data class Result(
        @Schema(description = "List of files in the current page")
        val files: List<FileSummary>,
        @Schema(description = "Total number of files")
        val totalElements: Long,
        @Schema(description = "Total number of pages")
        val totalPages: Int,
        @Schema(description = "Current page number (0-indexed)")
        val currentPage: Int,
        @Schema(description = "Number of items per page")
        val pageSize: Int,
    )

    @Schema(description = "Summary of a file")
    data class FileSummary(
        @Schema(description = "ID of the file")
        val fileId: UUID,
        @Schema(description = "Name of the file")
        val filename: String,
        @Schema(description = "Expected size in bytes")
        val expectedSize: Long,
        @Schema(description = "MIME type of the file")
        val mimeType: String,
        @Schema(description = "Upload status")
        val status: FileStatus,
        @Schema(description = "Creation timestamp")
        val createdAt: Instant,
        @Schema(description = "Last update timestamp")
        val updatedAt: Instant,
    )
}

data class GetFileDownloadUrlQuery(val fileId: UUID) : Query<GetFileDownloadUrlQuery.Result> {

    @Schema(name = "GetFileDownloadUrlResult", description = "Presigned download URL for a file")
    data class Result(
        @Schema(description = "ID of the file")
        val fileId: UUID,
        @Schema(description = "Name of the file")
        val filename: String,
        @Schema(description = "Presigned download URL")
        val downloadUrl: String,
    )
}
