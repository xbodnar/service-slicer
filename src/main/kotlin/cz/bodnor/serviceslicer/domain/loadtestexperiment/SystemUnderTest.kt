package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
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
    val name: String,
    // Reference to the docker-compose file
    val composeFileId: UUID,
    // Reference to the JAR file
    val jarFileId: UUID,
    // Description of this system configuration (e.g., "Baseline monolith", "3-service decomposition")
    val description: String? = null,
) : UpdatableEntity(id)

@Repository
interface SystemUnderTestRepository : JpaRepository<SystemUnderTest, UUID> {
    fun findByExperimentId(experimentId: UUID): List<SystemUnderTest>
}
