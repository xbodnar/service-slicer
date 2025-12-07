package cz.bodnor.serviceslicer.domain.decompositioncandidate

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class DecompositionCandidateReadService(
    private val repository: DecompositionCandidateRepository,
) : BaseFinderService<DecompositionCandidate>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = DecompositionCandidate::class

    @Transactional(readOnly = true)
    fun findAllByDecompositionJobId(decompositionJobId: UUID) =
        repository.findAllByDecompositionJobId(decompositionJobId)
}
