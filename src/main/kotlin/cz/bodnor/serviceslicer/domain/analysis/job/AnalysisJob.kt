package cz.bodnor.serviceslicer.domain.analysis.job

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Represents an analysis job for a project.
 */
@Entity
class AnalysisJob(
    id: UUID = UUID.randomUUID(),

    /**
     * The project being analyzed
     */
    val projectId: UUID,

    // TODO: Run configuration, compose.yaml, workload script
) : UpdatableEntity(id) {

    /**
     * Current status of the analysis job
     */
    @Enumerated(EnumType.STRING)
    var status: AnalysisJobStatus = AnalysisJobStatus.CREATED
        private set

    /**
     * When the analysis job started
     */
    var startedAt: Instant? = null
        private set

    /**
     * When the analysis job completed
     */
    var completedAt: Instant? = null
        private set
}

@Repository
interface AnalysisJobRepository : JpaRepository<AnalysisJob, UUID> {
    fun findByProjectId(projectId: UUID): AnalysisJob?
}

/**
 * Status of the analysis job
 */
enum class AnalysisJobStatus {
    CREATED,
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
}
