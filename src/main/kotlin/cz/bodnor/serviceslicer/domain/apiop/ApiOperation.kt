package cz.bodnor.serviceslicer.domain.apiop

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import io.swagger.v3.oas.models.media.Schema
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
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
    // Request body schema
    @JdbcTypeCode(SqlTypes.JSON)
    val requestBody: RequestBody? = null,
) : UpdatableEntity(id)

data class RequestBody(
    val content: Map<String, Schema<Any>>,
)

@Repository
interface ApiOperationRepository : JpaRepository<ApiOperation, UUID>
