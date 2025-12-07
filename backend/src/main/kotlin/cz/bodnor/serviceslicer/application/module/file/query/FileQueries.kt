package cz.bodnor.serviceslicer.application.module.file.query

import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

data class ListFilesQuery(
    val page: Int = 0,
    val size: Int = 10,
) : Query<Page<File>> {
    fun toPageable() = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTimestamp"))
}

data class GetFileDownloadUrlQuery(val fileId: UUID) : Query<GetFileDownloadUrlQuery.Result> {

    data class Result(
        val downloadUrl: String,
    )
}
