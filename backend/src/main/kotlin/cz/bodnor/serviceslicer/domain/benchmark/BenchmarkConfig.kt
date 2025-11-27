package cz.bodnor.serviceslicer.domain.benchmark

import java.util.UUID

/**
 * Configuration for running scalability assessments and load tests.
 * Contains OpenAPI specification, behavior models (user flows),
 * operational profiles, and k6 test configurations.
 */
data class BenchmarkConfig(
    val id: UUID = UUID.randomUUID(),
    // Reference to the OpenAPI specification file
    var openApiFileId: UUID,
    // Behavior models representing user flows/scenarios
    val behaviorModels: List<BehaviorModel> = emptyList(),
    // Operational profile defining load patterns and weights
    val operationalProfile: List<OperationalLoad> = emptyList(),
)

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
    // Sequence of API operations that comprise this user flow
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
    val body: Map<String, Any?> = emptyMap(),
    val save: Map<String, String> = emptyMap(),
    val operationId: String,
)

data class OperationalLoad(
    // Number of concurrent users
    val load: Int,
    // Frequency of this load (must sum to 1)
    val frequency: Double,
)
