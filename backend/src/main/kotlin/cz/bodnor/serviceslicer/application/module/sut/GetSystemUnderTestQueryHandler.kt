package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.query.GetSystemUnderTestQuery
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWithFiles
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetSystemUnderTestQueryHandler(
    private val sutReadService: SystemUnderTestReadService,
    private val fileReadService: FileReadService,
) : QueryHandler<SystemUnderTestWithFiles, GetSystemUnderTestQuery> {
    override val query = GetSystemUnderTestQuery::class

    override fun handle(query: GetSystemUnderTestQuery): SystemUnderTestWithFiles {
        val sut = sutReadService.getById(query.sutId)
        val fileIds = sut.getFileIds()
        val files = fileReadService.findAllByIds(fileIds)

        return sut.withFiles(files)
    }
}
