package cz.bodnor.serviceslicer.domain.sut

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a system configuration under test.
 * Each SUT has a docker-compose file and a JAR file that define the system's deployment.
 */
@Entity
class SystemUnderTest(
    var name: String,
    // Description of this system configuration (e.g., "Baseline monolith", "3-service decomposition")
    var description: String? = null,
    // Docker configuration
    @JdbcTypeCode(SqlTypes.JSON)
    var dockerConfig: DockerConfig,
    // Database seeding configurations (one per database for microservices)
    @JdbcTypeCode(SqlTypes.JSON)
    var databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) : UpdatableEntity()

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

@Repository
interface SystemUnderTestRepository : JpaRepository<SystemUnderTest, UUID>
