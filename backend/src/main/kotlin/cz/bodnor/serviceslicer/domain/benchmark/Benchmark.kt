package cz.bodnor.serviceslicer.domain.benchmark

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a benchmark that compares multiple system configurations
 * (e.g., baseline monolith vs decomposed microservices) under the same load test configuration.
 */
@Entity
class Benchmark(
    // Reference to the load test configuration
    @JdbcTypeCode(SqlTypes.JSON)
    var config: BenchmarkConfig,
    // Custom name to identify this benchmark
    val name: String,
    // Description of this benchmark
    val description: String? = null,
) : UpdatableEntity() {

    // Systems under test
    @OneToMany(mappedBy = "benchmarkId", orphanRemoval = true, cascade = [CascadeType.ALL])
    val systemsUnderTest: MutableList<SystemUnderTest> = mutableListOf()

    fun getSystemUnderTest(systemUnderTestId: UUID) = systemsUnderTest.find { it.id == systemUnderTestId }
        ?: error("SystemUnderTest with id $systemUnderTestId not found in benchmark $id")

    fun addSystemUnderTest(systemUnderTest: SystemUnderTest) {
        val containsBaseline = systemsUnderTest.any { it.isBaseline }
        require(!containsBaseline || !systemUnderTest.isBaseline) {
            "Can't add another baseline SUT"
        }
        systemsUnderTest.add(systemUnderTest)
    }

    fun removeSystemUnderTest(systemUnderTestId: UUID) {
        val sutToRemove = systemsUnderTest.find { it.id == systemUnderTestId } ?: return
        require(!sutToRemove.isBaseline) {
            "Can't remove baseline SUT"
        }
        systemsUnderTest.remove(sutToRemove)
    }
}

@Repository
interface BenchmarkRepository : JpaRepository<Benchmark, UUID>
