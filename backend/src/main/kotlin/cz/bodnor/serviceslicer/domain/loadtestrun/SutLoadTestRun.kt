package cz.bodnor.serviceslicer.domain.loadtestrun

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
class SutLoadTestRun(
    // Reference to the load test experiment
    val experimentId: UUID,

    // Reference to the system under test
    val systemUnderTestId: UUID,

    // Number of virtual users
    val targetVus: Int,

    val startTimestamp: Instant,
    val endTimestamp: Instant,

    // All operation-level metrics for this run
    @JdbcTypeCode(SqlTypes.JSON)
    val operationMeasurements: List<OperationRunMetrics> = emptyList(),
) : CreatableEntity() {

//    /**
//     * Σi Ni(λ, α) – total calls across all operations in this run.
//     */
//    val totalCallCount: Long
//        get() = operationMeasurements.sumOf { it.callCount }

//    /**
//     * νj(λ, α) – frequency share for a given operation in this run.
//     */
//    fun frequencyShare(operationId: String): Double {
//        val total = totalCallCount
//        if (total == 0L) return 0.0
//
//        val op = operationMeasurements.first { it.operationId == operationId }
//        return op.callCount.toDouble() / total.toDouble()
//    }
}

data class OperationRunMetrics(
    val operationId: String,

    // counts
    val totalRequests: Long,
    val failedRequests: Long,

    // latency
    val meanResponseTimeMs: BigDecimal,
    val stdDevResponseTimeMs: BigDecimal,

    // percentiles
    val p95DurationMs: BigDecimal,
    val p99DurationMs: BigDecimal,
)

@Repository
interface LoadTestRunRepository : JpaRepository<SutLoadTestRun, UUID> {
    fun findByExperimentId(experimentId: UUID): List<SutLoadTestRun>
    fun findBySystemUnderTestId(systemUnderTestId: UUID): List<SutLoadTestRun>
    fun findByExperimentIdAndSystemUnderTestId(
        experimentId: UUID,
        systemUnderTestId: UUID,
    ): List<SutLoadTestRun>
}
