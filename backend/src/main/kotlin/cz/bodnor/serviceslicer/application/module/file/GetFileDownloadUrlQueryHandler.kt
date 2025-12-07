package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.port.out.GenerateFileDownloadUrl
import cz.bodnor.serviceslicer.application.module.file.query.GetFileDownloadUrlQuery
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetFileDownloadUrlQueryHandler(
    private val fileReadService: FileReadService,
    private val generateFileDownloadUrl: GenerateFileDownloadUrl,
) : QueryHandler<GetFileDownloadUrlQuery.Result, GetFileDownloadUrlQuery> {
    override val query = GetFileDownloadUrlQuery::class

    override fun handle(query: GetFileDownloadUrlQuery): GetFileDownloadUrlQuery.Result {
        val file = fileReadService.getById(query.fileId)

        return GetFileDownloadUrlQuery.Result(
            downloadUrl = generateFileDownloadUrl(file.storageKey),
        )
    }
}
