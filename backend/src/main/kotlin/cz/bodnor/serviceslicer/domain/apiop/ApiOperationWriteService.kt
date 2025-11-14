package cz.bodnor.serviceslicer.domain.apiop

import cz.bodnor.serviceslicer.Tables.API_OPERATION
import org.jooq.DSLContext
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ApiOperationWriteService(
    private val repository: ApiOperationRepository,
    private val dslContext: DSLContext,
) {
    fun create(apiOperations: List<ApiOperation>) {
        repository.saveAll(apiOperations)
    }

    fun deleteByOpenApiFileId(openApiFileId: UUID) {
        dslContext.delete(API_OPERATION).where(API_OPERATION.OPEN_API_FILE_ID.eq(openApiFileId))
    }
}
