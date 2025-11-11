package cz.bodnor.serviceslicer.adapter.out.jooq

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.Tables.API_OPERATION
import cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out.SaveApiOperations
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.impl.DSL
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class SaveApiOperationsJooq(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
) : SaveApiOperations {

    override fun invoke(apiOperations: List<ApiOperation>) {
        val now = Instant.now()
        val rows =
            apiOperations.map { apiOperation ->
                DSL.row(
                    apiOperation.id,
                    0, // version
                    now,
                    now,
                    apiOperation.openApiFileId,
                    apiOperation.operationId,
                    apiOperation.method,
                    apiOperation.path,
                    apiOperation.name,
                    JSONB.valueOf(objectMapper.writeValueAsString(apiOperation.requestBody)),
                )
            }

        dslContext
            .insertInto(
                API_OPERATION,
                API_OPERATION.ID,
                API_OPERATION.VERSION,
                API_OPERATION.CREATED_AT,
                API_OPERATION.UPDATED_AT,
                API_OPERATION.OPEN_API_FILE_ID,
                API_OPERATION.OPERATION_ID,
                API_OPERATION.METHOD,
                API_OPERATION.PATH,
                API_OPERATION.NAME,
                API_OPERATION.REQUEST_BODY,
            ).valuesOfRows(rows)
            .onConflict(API_OPERATION.OPEN_API_FILE_ID, API_OPERATION.METHOD, API_OPERATION.PATH)
            .doUpdate()
            .set(API_OPERATION.METHOD, DSL.excluded(API_OPERATION.METHOD))
            .set(API_OPERATION.PATH, DSL.excluded(API_OPERATION.PATH))
            .set(API_OPERATION.NAME, DSL.excluded(API_OPERATION.NAME))
            .set(API_OPERATION.REQUEST_BODY, DSL.excluded(API_OPERATION.REQUEST_BODY))
            .execute()
    }
}
