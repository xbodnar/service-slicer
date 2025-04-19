package cz.bodnor.serviceslicer.domain.analysis.job

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AnalysisJobCreateService(
    private val analysisJobRepository: AnalysisJobRepository,
) {

    fun create(
        projectId: UUID,
        analysisType: AnalysisType,
    ): AnalysisJob = analysisJobRepository.save(
        AnalysisJob(
            projectId = projectId,
            analysisType = analysisType,
        ),
    )
}
