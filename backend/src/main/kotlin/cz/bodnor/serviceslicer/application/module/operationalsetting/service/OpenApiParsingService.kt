package cz.bodnor.serviceslicer.application.module.operationalsetting.service

import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.apiop.ApiParameter
import cz.bodnor.serviceslicer.domain.apiop.ApiResponse
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
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
        isResolve = false
        isResolveFully = false
        isFlatten = false
    }

    fun parse(openApiFileId: UUID): List<ApiOperation> = diskOperations.withFile(openApiFileId) { openApiPath ->
        val parseResult = parser.readLocation(openApiPath.toUri().toString(), null, parserOptions)
        val openAPI = parseResult.openAPI

        openAPI.paths.flatMap { (path, pathItem) ->
            pathItem.readOperationsMap().map { (method, operation) ->
                ApiOperation(
                    openApiFileId = openApiFileId,
                    operationId = operation.operationId ?: error("Operation ID not found for operation $operation"),
                    method = method.name,
                    path = path,
                    parameters = operation.parameters?.mapNotNull { param ->
                        param?.schema?.`$ref`?.let { schemaName ->
                            ApiParameter(
                                name = param.name,
                                `in` = param.`in`,
                                required = param.required ?: false,
                                schema = schemaName.extractSchema(openAPI),
                            )
                        }
                    } ?: emptyList(),
                    requestBody = operation.requestBody?.content?.mapValues { content ->
                        content.value.schema?.`$ref`?.extractSchema(openAPI)
                    } ?: emptyMap(),
                    responses = operation.responses?.mapValues { (_, response) ->
                        ApiResponse(
                            content = response.content?.mapValues { content ->
                                content.value.schema?.`$ref`?.extractSchema(openAPI)
                            } ?: emptyMap(),
                        )
                    } ?: emptyMap(),
                )
            }
        }
    }

    private fun String.extractSchema(openAPI: OpenAPI): Schema<*> {
        val schemaName = this.substringAfterLast("/")
        return openAPI.components.schemas[schemaName] ?: error("Reference to schema object $schemaName not found")
    }
}
