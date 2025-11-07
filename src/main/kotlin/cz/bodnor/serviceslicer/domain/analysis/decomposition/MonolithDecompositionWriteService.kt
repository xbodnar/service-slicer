package cz.bodnor.serviceslicer.domain.analysis.decomposition

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MonolithDecompositionWriteService(
    private val decompositionRepository: MonolithDecompositionRepository,
    private val serviceBoundaryRepository: ServiceBoundaryRepository,
) {

    @Transactional
    fun save(
        decomposition: MonolithDecomposition,
        serviceBoundaries: List<ServiceBoundary>,
    ): MonolithDecomposition {
        decompositionRepository.save(decomposition)

        serviceBoundaries.forEach {
            require(it.monolithDecompositionId == decomposition.id) {
                "Service boundary must belong to the given decomposition"
            }
            serviceBoundaryRepository.save(it)
        }

        return decomposition
    }
}
