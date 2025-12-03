package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.query.ListFilesQuery
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class ListFilesQueryHandler(
    private val fileReadService: FileReadService,
) : QueryHandler<ListFilesQuery.Result, ListFilesQuery> {
    override val query = ListFilesQuery::class

    override fun handle(query: ListFilesQuery): ListFilesQuery.Result {
        val pageable = PageRequest.of(
            query.page,
            query.size,
            Sort.by(Sort.Direction.DESC, "createdTimestamp"),
        )

        val page = fileReadService.findAll(pageable)

        return ListFilesQuery.Result(
            files = page.content.map {
                ListFilesQuery.FileSummary(
                    fileId = it.id,
                    filename = it.filename,
                    expectedSize = it.fileSize,
                    mimeType = it.mimeType,
                    status = it.status,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
        )
    }
}
