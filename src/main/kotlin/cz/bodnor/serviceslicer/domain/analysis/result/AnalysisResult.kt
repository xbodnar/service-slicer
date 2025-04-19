package cz.bodnor.serviceslicer.domain.analysis.result

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents the results of an analysis job.
 */
@Entity
class AnalysisResult(
    id: UUID = UUID.randomUUID(),
    /**
     * The analysis job that produced this result
     */
    val analysisJobId: UUID,
) : UpdatableEntity(id) {
    /**
     * Path to the dependency graph file
     */
    var dependencyGraphPath: String? = null
        private set

    /**
     * Serialized dependency graph data
     */
    var dependencyGraphData: String? = null
        private set

    /**
     * Suggested microservices as JSON
     */
    var suggestedMicroservicesJson: String? = null
        private set
}

@Repository
interface AnalysisResultRepository : JpaRepository<AnalysisResult, UUID>
