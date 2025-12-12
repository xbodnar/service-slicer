package cz.bodnor.serviceslicer.domain.operationalsetting

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.file.File
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

/**
 * Configuration for running scalability assessments and load tests.
 * Contains OpenAPI specification, behavior models (user flows),
 * operational profiles, and k6 test configurations.
 */
@Entity
class OperationalSetting(
    // Name of the operational setting
    val name: String,
    // Description of the operational setting
    val description: String? = null,
    // Reference to the OpenAPI specification file
    @ManyToOne
    var openApiFile: File,
    // Behavior models representing user flows/scenarios
    @JdbcTypeCode(SqlTypes.JSON)
    var usageProfile: List<BehaviorModel> = emptyList(),
    // Operational profile defining load patterns and weights
    @JdbcTypeCode(SqlTypes.JSON)
    var operationalProfile: Map<Int, BigDecimal> = emptyMap(),
) : UpdatableEntity()

@Repository
interface OperationalSettingRepository : JpaRepository<OperationalSetting, UUID>

/**
 * Represents a user behavior model (flow/scenario).
 */
data class BehaviorModel(
    // Arbitrary ID such as bm1, bm2 so users can identify this behavior model
    val id: String,
    // Name of the actor persona this behavior model represents
    val actor: String,
    // Probability for this behaviour model. Must be between 0 and 1 and sum of frequencies
    // across all behavior models must be 1.
    val frequency: BigDecimal,
    // Sequence of API operations that comprise this user flow
    val steps: List<ApiRequest>,
)

/**
 * Represents a single API request within a behavior model.
 */
data class ApiRequest(
    /**
     * ID of the API operation taken from the OpenApi specification, that this request corresponds to.
     */
    val operationId: String,
    /**
     * HTTP method of the API operation taken from the OpenApi specification, that this request corresponds to.
     */
    val method: String,
    /**
     * Path of the API operation taken from the OpenApi specification, that this request corresponds to.
     */
    val path: String,
    /**
     * Name of the component this request belongs to.
     */
    val component: String? = null,
    /**
     * Map of HTTP Headers (key-value pairs), may contain variables
     */
    val headers: Map<String, String> = emptyMap(),
    /**
     * Map of Query Parameters (key-value pairs), may contain variables
     */
    val params: Map<String, String> = emptyMap(),
    /**
     * Request body (JSON Object), may contain variables
     */
    val body: Map<String, Any?> = emptyMap(),
    /**
     * Map of JSONPath selectors to extract variables from the response. Key is the variable name, value is the
     * JSONPath selector. If a subsequent requests are using any variables, it must be defined in a previous step.
     * Example: {"articleId": "$.articles[0].id"}
     */
    val save: Map<String, String> = emptyMap(),
    /**
     * Minimum think time in milliseconds
     */
    val waitMsFrom: Int,
    /**
     * Maximum think time in milliseconds
     */
    val waitMsTo: Int,
)
