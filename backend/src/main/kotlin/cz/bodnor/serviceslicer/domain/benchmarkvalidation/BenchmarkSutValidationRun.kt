package cz.bodnor.serviceslicer.domain.benchmarkvalidation

import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkSystemUnderTest
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.OneToOne
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Entity
class BenchmarkSutValidationRun(
    @OneToOne
    @JoinColumns(
        JoinColumn(
            name = "benchmark_system_under_test_benchmark_id",
            referencedColumnName = "benchmark_id",
        ),
        JoinColumn(
            name = "benchmark_system_under_test_system_under_test_id",
            referencedColumnName = "system_under_test_id",
        ),
    )
    val benchmarkSystemUnderTest: BenchmarkSystemUnderTest,
) : UpdatableEntity() {

    var errorMessage: String? = null
        private set

    @Column(name = "k6_output")
    var k6Output: String? = null
        private set

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        private set

    var startTimestamp: Instant? = null
        private set

    var endTimestamp: Instant? = null
        private set

    fun queued() {
        require(this.status != JobStatus.RUNNING) { "Cannot queue validation run in status $status" }
        this.status = JobStatus.PENDING
        this.startTimestamp = null
        this.endTimestamp = null
        this.errorMessage = null
        this.k6Output = null
    }

    fun started() {
        this.status = JobStatus.RUNNING
        this.startTimestamp = Instant.now()
    }

    fun completed(k6Output: String) {
        this.status = JobStatus.COMPLETED
        this.endTimestamp = Instant.now()
        this.k6Output = k6Output
    }

    fun failed(errorMessage: String) {
        this.status = JobStatus.FAILED
        this.endTimestamp = Instant.now()
        this.errorMessage = errorMessage
    }
}

@Repository
interface BenchmarkSutValidationRunRepository : JpaRepository<BenchmarkSutValidationRun, UUID> {
    fun findFirstByStatusOrderByCreatedTimestampAsc(status: JobStatus): BenchmarkSutValidationRun?
}
