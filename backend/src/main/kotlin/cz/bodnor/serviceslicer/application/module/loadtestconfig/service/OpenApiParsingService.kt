package cz.bodnor.serviceslicer.application.module.loadtestconfig.service

import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.apiop.RequestBody
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class OpenApiParsingService(
    private val diskOperations: DiskOperations,
) {

    private val parser = OpenAPIV3Parser()

    private val parserOptions = ParseOptions().apply {
        isResolve = true
        isResolveFully = true
    }

    fun parse(openApiFileId: UUID): List<ApiOperation> = diskOperations.withFile(openApiFileId) {
        val parseResult = parser.readLocation(it.toUri().toString(), null, parserOptions)
        val openAPI = parseResult.openAPI

        var idCounter = 1

        openAPI.paths.flatMap { (path, pathItem) ->
            pathItem.readOperationsMap().map { (method, operation) ->
                ApiOperation(
                    openApiFileId = openApiFileId,
                    operationId = "o${idCounter++}",
                    method = method.name,
                    path = path,
                    name = operation.operationId ?: "${method.name} $path",
                    requestBody = RequestBody(
                        content = operation.requestBody?.content?.mapValues { content ->
                            content.value.schema
                        } ?: emptyMap(),
                    ),
                )
            }
        }
    }
}
