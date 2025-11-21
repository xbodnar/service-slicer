package cz.bodnor.serviceslicer.domain.loadtestconfig

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
    val component: String? = null,
    val operationId: String,
)
