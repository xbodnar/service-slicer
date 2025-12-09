package cz.bodnor.serviceslicer.domain.decomposition

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import org.springframework.stereotype.Service

@Service
class DecompositionJobReadService(
    private val repository: DecompositionJobRepository,
) : BaseFinderService<DecompositionJob>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = DecompositionJob::class

    fun findOldestPending() = repository.findFirstByStatusOrderByCreatedTimestampAsc(JobStatus.PENDING)
}
