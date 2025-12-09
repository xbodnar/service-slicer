package cz.bodnor.serviceslicer.domain.decomposition

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.job.JobStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ManyToOne
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Represents a decomposition of a monolithic application into microservices
 */
@Entity
class DecompositionJob(

    val name: String,

    @ManyToOne
    val monolithArtifact: MonolithArtifact,
) : UpdatableEntity() {

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING
        private set

    var startTimestamp: Instant? = null
        private set

    var endTimestamp: Instant? = null
        private set

    fun started() {
        this.startTimestamp = Instant.now()
        this.status = JobStatus.RUNNING
    }

    fun queued() {
        this.status = JobStatus.PENDING
    }

    fun completed() {
        this.status = JobStatus.COMPLETED
        this.endTimestamp = Instant.now()
    }

    fun failed() {
        this.status = JobStatus.FAILED
        this.endTimestamp = Instant.now()
    }
}

@Repository
interface DecompositionJobRepository : JpaRepository<DecompositionJob, UUID>
