package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.application.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJob
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJobRepository
import org.springframework.stereotype.Service

@Service
class AnalysisJobFinderService(
    private val repository: AnalysisJobRepository,
) : BaseFinderService<AnalysisJob>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = AnalysisJob::class
}
