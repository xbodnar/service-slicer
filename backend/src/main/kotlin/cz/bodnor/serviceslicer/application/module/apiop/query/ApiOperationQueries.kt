package cz.bodnor.serviceslicer.application.module.apiop.query

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.util.UUID

data class ListApiOperationsQuery(
    val openApiFileId: UUID,
) : Query<List<ApiOperation>>
