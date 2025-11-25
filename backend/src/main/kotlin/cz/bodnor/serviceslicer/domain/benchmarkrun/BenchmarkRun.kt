package cz.bodnor.serviceslicer.domain.benchmarkrun

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class BenchmarkRun(
    val benchmarkId: UUID,
    @JdbcTypeCode(SqlTypes.JSON)
    val sutLoadTestRuns: MutableList<SutLoadTestRun> = mutableListOf(),
) : UpdatableEntity() {
    @Enumerated(EnumType.STRING)
    private var state: BenchmarkRunState = BenchmarkRunState.PENDING

    fun markFailed() {
        state = BenchmarkRunState.FAILED
    }

    fun markCompleted() {
        state = BenchmarkRunState.COMPLETED
    }
}

@Repository
interface BenchmarkRunRepository : JpaRepository<BenchmarkRun, UUID> {
    fun findByBenchmarkId(benchmarkId: UUID): BenchmarkRun?

    fun existsByBenchmarkId(benchmarkId: UUID): Boolean
}

enum class BenchmarkRunState {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
}
