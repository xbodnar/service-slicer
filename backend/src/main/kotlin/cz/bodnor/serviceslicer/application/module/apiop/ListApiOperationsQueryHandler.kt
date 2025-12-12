package cz.bodnor.serviceslicer.application.module.apiop

import cz.bodnor.serviceslicer.application.module.apiop.query.ListApiOperationsQuery
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class ListApiOperationsQueryHandler(
    private val apiOperationReadService: ApiOperationReadService,
) : QueryHandler<List<ApiOperation>, ListApiOperationsQuery> {

    override val query = ListApiOperationsQuery::class

    override fun handle(query: ListApiOperationsQuery): List<ApiOperation> =
        apiOperationReadService.getByOpenApiFileId(query.openApiFileId)
}
