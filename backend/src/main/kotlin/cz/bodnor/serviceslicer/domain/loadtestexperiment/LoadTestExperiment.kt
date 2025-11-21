package cz.bodnor.serviceslicer.domain.loadtestexperiment

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import org.jooq.meta.derby.sys.Sys
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

    val baselineSut: SystemUnderTest
        get() = _systemsUnderTest.find { it.isBaseline } ?: error("No baseline SUT found")

    val targetSuts: List<SystemUnderTest>
        get() = _systemsUnderTest.filter { !it.isBaseline }

    fun addSystemUnderTest(systemUnderTest: SystemUnderTest) {
        val containsBaseline = _systemsUnderTest.any { it.isBaseline }
        require(!containsBaseline || !systemUnderTest.isBaseline) {
            "Can't add another baseline SUT"
        }
        _systemsUnderTest.add(systemUnderTest)
    }

    fun removeSystemUnderTest(systemUnderTestId: UUID) {
        val sutToRemove = _systemsUnderTest.find { it.id == systemUnderTestId } ?: return
        require(!sutToRemove.isBaseline) {
            "Can't remove baseline SUT"
        }
        _systemsUnderTest.remove(sutToRemove)
    }
}

@Repository
interface LoadTestExperimentRepository : JpaRepository<LoadTestExperiment, UUID>
