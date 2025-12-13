package cz.bodnor.serviceslicer.domain.benchmarkvalidation

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.job.JobStatus
import org.springframework.stereotype.Service

@Service
class BenchmarkSutValidationRunReadService(
    private val repository: BenchmarkSutValidationRunRepository,
) : BaseFinderService<BenchmarkSutValidationRun>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = BenchmarkSutValidationRun::class

    fun findOldestPending() = repository.findFirstByStatusOrderByCreatedTimestampAsc(JobStatus.PENDING)
}
