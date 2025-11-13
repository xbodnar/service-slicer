package cz.bodnor.serviceslicer.domain.apiop

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApiOperationWriteService(
    private val repository: ApiOperationRepository,
) {
    fun create(apiOperations: List<ApiOperation>) {
        repository.saveAll(apiOperations)
    }

    fun deleteByOpenApiFileId(openApiFileId: UUID) {
        repository.deleteByOpenApiFileId(openApiFileId)
    }
}
