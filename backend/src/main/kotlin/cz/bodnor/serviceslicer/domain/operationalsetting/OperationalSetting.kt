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

data class ApiRequest(
    val method: String,
    val path: String,
    val component: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
    val body: Map<String, Any?> = emptyMap(),
    val save: Map<String, String> = emptyMap(),
    val waitMsFrom: Int,
    val waitMsTo: Int,
    val operationId: String,
)
