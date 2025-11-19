package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a load test experiment that compares multiple system configurations
 * (e.g., baseline monolith vs decomposed microservices) under the same load test configuration.
 */
@Entity
class LoadTestExperiment(
    id: UUID = UUID.randomUUID(),
    // Reference to the load test configuration
    var loadTestConfigId: UUID,
    // Custom name to identify this experiment
    val name: String,
    // Description of this experiment
    val description: String? = null,
) : UpdatableEntity(id) {
    @OneToMany(mappedBy = "experimentId", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _systemsUnderTest: MutableList<SystemUnderTest> = mutableListOf()

    val systemsUnderTest: List<SystemUnderTest>
        get() = _systemsUnderTest.toList()

    fun addSystemUnderTest(systemUnderTest: SystemUnderTest) {
        _systemsUnderTest.add(systemUnderTest)
    }

    fun removeSystemUnderTest(systemUnderTestId: UUID) {
        _systemsUnderTest.removeIf { it.id == systemUnderTestId }
    }
}

@Repository
interface LoadTestExperimentRepository : JpaRepository<LoadTestExperiment, UUID>
