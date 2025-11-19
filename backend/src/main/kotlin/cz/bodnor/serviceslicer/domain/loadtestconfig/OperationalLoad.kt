package cz.bodnor.serviceslicer.domain.loadtestconfig

data class OperationalLoad(
    // Number of concurrent users
    val load: Int,
    // Frequency of this load (must sum to 1)
    val frequency: Double,
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
