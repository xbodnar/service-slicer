package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
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
    // Reference to the docker-compose file
    var composeFileId: UUID,
    // Reference to the JAR file
    var jarFileId: UUID,
    // Reference to the SQL seed file
    var sqlSeedFileId: UUID? = null,
    // Description of this system configuration (e.g., "Baseline monolith", "3-service decomposition")
    var description: String? = null,
    // Health check endpoint path (e.g., "/actuator/health")
    var healthCheckPath: String,
    // Port on which the application is exposed (based on docker-compose)
    var appPort: Int,
    // Startup timeout in seconds
    var startupTimeoutSeconds: Long,
) : UpdatableEntity(id)

@Repository
interface SystemUnderTestRepository : JpaRepository<SystemUnderTest, UUID> {
    fun findByExperimentId(experimentId: UUID): List<SystemUnderTest>
}
