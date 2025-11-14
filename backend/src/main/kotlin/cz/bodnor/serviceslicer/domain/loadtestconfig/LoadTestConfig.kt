package cz.bodnor.serviceslicer.domain.loadtestconfig

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Configuration for running scalability assessments and load tests.
 * Contains OpenAPI specification, behavior models (user flows),
 * operational profiles, and k6 test configurations.
 */
@Entity
class LoadTestConfig(
    id: UUID = UUID.randomUUID(),
    // Reference to the OpenAPI specification file
    var openApiFileId: UUID,
    // Behavior models representing user flows/scenarios
    @JdbcTypeCode(SqlTypes.JSON)
    var behaviorModels: List<BehaviorModel> = emptyList(),
    // Operational profile defining load patterns and weights
    @JdbcTypeCode(SqlTypes.JSON)
    var operationalProfile: OperationalProfile? = null,
    // k6 test configuration (VUs, duration, thresholds, etc.)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "k6_configuration")
    val k6Configuration: K6Configuration? = null,
) : UpdatableEntity(id)

/**
 * Represents a user behavior model (flow/scenario).
 */
data class BehaviorModel(
    // Arbitrary ID such as bm1, bm2 so users can identify this behavior model
    val id: String,
    // Name of the actor persona this behavior model represents
    val actor: String,
    // Usage profile (percentage of total traffic) for this behaviour model. Must be between 0 and 1 and sum of all
    // usage profiles must be 1.
    val usageProfile: Double,
    // Sequence of API operations that comprise this user flow. The IDs refer to the operationId in the ApiOperation
    // entities for this OpenApi file.
    // Example: ["o1", "o2", "o3"]
    val steps: List<ApiRequest>,
    // Think time range (in milliseconds)
    val thinkFrom: Int,
    val thinkTo: Int,
)

data class ApiRequest(
    val method: String,
    val path: String,
    val headers: Map<String, String> = emptyMap(),
    val params: Map<String, String> = emptyMap(),
    val body: String? = null,
)

/**
 * Operational profile defining load patterns and weights.
 */
data class OperationalProfile(
    // List of pairs of load (e.g., 25, 50, 100, 150, 200) and frequencies (must sum to 1)
    val loadsToFreq: List<Pair<Int, Double>>,
)

/**
 * k6-specific test configuration.
 */
data class K6Configuration(
    // Number of virtual users
    val vus: Int,
    // Test duration (e.g., "30s", "5m")
    val duration: String,
    // Ramp-up time (e.g., "10s")
    val rampUp: String? = null,
    // Performance thresholds
//    val thresholds: Map<String, List<String>>? = null,
    // Additional k6 options
//    val options: Map<String, Any>? = null,
)

@Repository
interface LoadTestConfigurationRepository : JpaRepository<LoadTestConfig, UUID>
