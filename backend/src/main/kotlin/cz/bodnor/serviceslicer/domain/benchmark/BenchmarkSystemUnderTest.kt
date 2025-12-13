package cz.bodnor.serviceslicer.domain.benchmark

import cz.bodnor.serviceslicer.domain.benchmarkvalidation.BenchmarkSutValidationRun
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Join table entity representing the many-to-many relationship between Benchmark and SystemUnderTest.
 * Each combination of Benchmark + SystemUnderTest is unique and can have its own validation result.
 */
@Entity
class BenchmarkSystemUnderTest(
    @EmbeddedId
    val id: BenchmarkSystemUnderTestId,

    @ManyToOne
    @MapsId("benchmarkId")
    val benchmark: Benchmark,

    @ManyToOne
    @MapsId("systemUnderTestId")
    val systemUnderTest: SystemUnderTest,

    val isBaseline: Boolean,
) {
    @OneToOne(mappedBy = "benchmarkSystemUnderTest")
    var benchmarkSutValidationRun: BenchmarkSutValidationRun? = null
        private set
}

/**
 * Composite primary key for BenchmarkSystemUnderTest.
 */
@Embeddable
data class BenchmarkSystemUnderTestId(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
) : Serializable

@Repository
interface BenchmarkSystemUnderTestRepository : JpaRepository<BenchmarkSystemUnderTest, BenchmarkSystemUnderTestId>

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
