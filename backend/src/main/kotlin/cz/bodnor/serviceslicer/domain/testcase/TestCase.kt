package cz.bodnor.serviceslicer.domain.testcase

import com.fasterxml.jackson.databind.JsonNode
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@MappedSuperclass
abstract class TestCase(
    val load: Int,
) : UpdatableEntity() {

    var startTimestamp: Instant? = null
        protected set

    var endTimestamp: Instant? = null
        protected set

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        protected set

    @Column(name = "k6_output")
    var k6Output: String? = null
        protected set

    @JdbcTypeCode(SqlTypes.JSON)
    var jsonSummary: JsonNode? = null
        protected set

    fun started() {
        this.startTimestamp = Instant.now()
        this.status = JobStatus.RUNNING
    }

    abstract fun completed(
        performanceMetrics: List<QueryLoadTestMetrics.PerformanceMetrics>,
        k6Output: String,
        jsonSummary: JsonNode?,
    )

    fun failed() {
        this.status = JobStatus.FAILED
        this.endTimestamp = Instant.now()
    }
}
