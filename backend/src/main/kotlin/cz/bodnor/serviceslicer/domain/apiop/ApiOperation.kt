package cz.bodnor.serviceslicer.domain.apiop

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import io.swagger.v3.oas.models.media.Schema
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class ApiOperation(
    id: UUID = UUID.randomUUID(),
    // ID of the OpenAPI file this operation belongs to
    val openApiFileId: UUID,
    // User readable operation id, such as o1, o2, etc.
    val operationId: String,
    // HTTP method
    val method: String,
    // Path with variables, such as /users/{id}
    val path: String,
    // User readable name, such as "Get user by ID"
    val name: String,
    // Request parameters (path, query, header, cookie)
    @JdbcTypeCode(SqlTypes.JSON)
    val parameters: List<ApiParameter> = emptyList(),
    // Request body schema
    @JdbcTypeCode(SqlTypes.JSON)
    val requestBody: Map<String, Schema<*>?>? = null,
    // Response schemas by status code
    @JdbcTypeCode(SqlTypes.JSON)
    val responses: Map<String, ApiResponse> = emptyMap(),
) : UpdatableEntity(id)

data class ApiParameter(
    val name: String,
    val `in`: String, // "path", "query", "header", "cookie"
    val required: Boolean,
    val schema: Schema<*>,
)

data class ApiResponse(
    val content: Map<String, Schema<*>?>,
)

@Repository
interface ApiOperationRepository : JpaRepository<ApiOperation, UUID> {

    fun findByOpenApiFileId(openApiFileId: UUID): List<ApiOperation>

    @Modifying
    fun deleteByOpenApiFileId(openApiFileId: UUID)
}
