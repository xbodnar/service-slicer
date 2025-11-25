package cz.bodnor.serviceslicer.domain.benchmark

import java.time.Instant
import java.util.UUID

/**
 * Represents a system configuration under test.
 * Each SUT has a docker-compose file and a JAR file that define the system's deployment.
 */
data class SystemUnderTest(
    val id: UUID = UUID.randomUUID(),
    // Custom name to identify this system configuration
    var name: String,
    // Description of this system configuration (e.g., "Baseline monolith", "3-service decomposition")
    var description: String? = null,
    // Baseline SUT
    val isBaseline: Boolean,
    // Docker configuration
    var dockerConfig: DockerConfig,
    // Database seeding configuration
    var databaseSeedConfig: DatabaseSeedConfig? = null,
    // Result of the last validation run
    var validationResult: ValidationResult? = null,
)

data class ValidationResult(
    val validationState: ValidationState = ValidationState.PENDING,
    val timestamp: Instant = Instant.now(),
    val errorMessage: String? = null,
    val k6Output: String? = null,
)

enum class ValidationState {
    PENDING,
    VALID,
    INVALID,
}

data class DatabaseSeedConfig(
    // Reference to the SQL seed file
    val sqlSeedFileId: UUID,
    // Database container name in docker-compose
    val dbContainerName: String,
    // Database port inside container
    val dbPort: Int,
    // Database name
    val dbName: String,
    // Database username
    val dbUsername: String,
)

data class DockerConfig(
    // Reference to the docker-compose file
    val composeFileId: UUID,
    // Health check endpoint path (e.g., "/actuator/health")
    val healthCheckPath: String,
    // Port on which the application is exposed (based on docker-compose)
    val appPort: Int,
    // Startup timeout in seconds
    val startupTimeoutSeconds: Long,
)
