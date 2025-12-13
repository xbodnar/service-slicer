package cz.bodnor.serviceslicer.domain.benchmark

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a benchmark that compares multiple system configurations
 * (e.g., baseline monolith vs decomposed microservices) under the same load test configuration.
 */
@Entity
class Benchmark(
    // Custom name to identify this benchmark
    var name: String,
    // Description of this benchmark
    var description: String? = null,
    // Reference to the load test configuration
    @ManyToOne
    var operationalSetting: OperationalSetting,
) : UpdatableEntity() {

    @OneToMany(mappedBy = "benchmark", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _systemsUnderTest: MutableSet<BenchmarkSystemUnderTest> = mutableSetOf()

    val systemsUnderTest: Set<BenchmarkSystemUnderTest>
        get() = _systemsUnderTest.toSet()

    fun addSystemUnderTest(
        systemUnderTest: SystemUnderTest,
        isBaseline: Boolean,
    ) {
        val id = BenchmarkSystemUnderTestId(
            benchmarkId = this.id,
            systemUnderTestId = systemUnderTest.id,
        )
        val benchmarkSut = BenchmarkSystemUnderTest(
            id = id,
            benchmark = this,
            systemUnderTest = systemUnderTest,
            isBaseline = isBaseline,
        )
        _systemsUnderTest.add(benchmarkSut)
    }

    fun removeSystemUnderTest(systemUnderTest: SystemUnderTest) {
        _systemsUnderTest.removeIf { it.systemUnderTest.id == systemUnderTest.id }
    }
}

@Repository
interface BenchmarkRepository : JpaRepository<Benchmark, UUID>
