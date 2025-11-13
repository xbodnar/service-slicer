package cz.bodnor.serviceslicer.domain.apiop

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApiOperationReadService(
    private val repository: ApiOperationRepository,
) : BaseFinderService<ApiOperation>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = ApiOperation::class

    fun getByOpenApiFileId(openApiFileId: UUID) = repository.findByOpenApiFileId(openApiFileId)
}
