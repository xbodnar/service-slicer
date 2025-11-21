package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a system configuration under test.
 * Each SUT has a docker-compose file and a JAR file that define the system's deployment.
 */
@Entity
class SystemUnderTest(
    id: UUID = UUID.randomUUID(),
    // Reference to the parent experiment
    val experimentId: UUID,
    // Custom name to identify this system configuration
    var name: String,
    // Description of this system configuration (e.g., "Baseline monolith", "3-service decomposition")
    var description: String? = null,
    // Baseline SUT
    val isBaseline: Boolean,
    // Docker configuration
    @Embedded
    var dockerConfig: DockerConfig,
    // Database seeding configuration
    @Embedded
    var databaseSeedConfig: DatabaseSeedConfig? = null,
) : UpdatableEntity(id)

@Repository
interface SystemUnderTestRepository : JpaRepository<SystemUnderTest, UUID> {
    fun findByExperimentId(experimentId: UUID): List<SystemUnderTest>
}

@Embeddable
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

@Embeddable
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
