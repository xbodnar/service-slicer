package cz.bodnor.serviceslicer.application.module.file.query

import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.time.Instant
import java.util.UUID

data class ListFilesQuery(
    val page: Int = 0,
    val size: Int = 20,
) : Query<ListFilesQuery.Result> {

    data class Result(
        val files: List<FileSummary>,
        val totalElements: Long,
        val totalPages: Int,
        val currentPage: Int,
        val pageSize: Int,
    )

    data class FileSummary(
        val fileId: UUID,
        val filename: String,
        val expectedSize: Long,
        val mimeType: String,
        val status: FileStatus,
        val createdAt: Instant,
        val updatedAt: Instant,
    )
}

data class GetFileDownloadUrlQuery(val fileId: UUID) : Query<GetFileDownloadUrlQuery.Result> {

    data class Result(
        val fileId: UUID,
        val filename: String,
        val downloadUrl: String,
    )
}
