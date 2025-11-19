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
    var operationalProfile: List<OperationalLoad> = emptyList(),
    // k6 test configuration (VUs, duration, thresholds, etc.)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "k6_configuration")
    val k6Configuration: K6Configuration? = null,
) : UpdatableEntity(id)

@Repository
interface LoadTestConfigurationRepository : JpaRepository<LoadTestConfig, UUID>
