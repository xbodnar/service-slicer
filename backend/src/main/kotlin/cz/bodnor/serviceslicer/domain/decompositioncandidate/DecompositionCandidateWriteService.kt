package cz.bodnor.serviceslicer.domain.decompositioncandidate

import org.springframework.stereotype.Service

@Service
class DecompositionCandidateWriteService(
    private val repository: DecompositionCandidateRepository,
) {

    fun save(decompositionCandidate: DecompositionCandidate) = repository.save(decompositionCandidate)
}
