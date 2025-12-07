package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.query.ListFilesQuery
import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class ListFilesQueryHandler(
    private val fileReadService: FileReadService,
) : QueryHandler<Page<File>, ListFilesQuery> {

    override val query = ListFilesQuery::class

    override fun handle(query: ListFilesQuery): Page<File> = fileReadService.findAll(query.toPageable())
}
