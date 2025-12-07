package cz.bodnor.serviceslicer.domain.benchmark

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
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
    // Baseline system under test
    @OneToOne
    val baselineSut: SystemUnderTest,
    // Target system under test
    @OneToOne
    val targetSut: SystemUnderTest,
) : UpdatableEntity() {

    @JdbcTypeCode(SqlTypes.JSON)
    var baselineSutValidationResult: ValidationResult? = null
        private set

    @JdbcTypeCode(SqlTypes.JSON)
    var targetSutValidationResult: ValidationResult? = null
        private set

    fun startValidationRun(systemUnderTestId: UUID) {
        when (systemUnderTestId) {
            baselineSut.id -> baselineSutValidationResult = ValidationResult()
            targetSut.id -> targetSutValidationResult = ValidationResult()
            else -> error("SUT $systemUnderTestId not found in benchmark $this")
        }
    }

    fun completeValidationRun(
        systemUnderTestId: UUID,
        result: ValidationResult,
    ) {
        when (systemUnderTestId) {
            baselineSut.id -> baselineSutValidationResult = result
            targetSut.id -> targetSutValidationResult = result
            else -> error("SUT $systemUnderTestId not found in benchmark $this")
        }
    }
}

@Repository
interface BenchmarkRepository : JpaRepository<Benchmark, UUID>

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
