package cz.bodnor.serviceslicer.domain.graph

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClassNodeReadService(
    private val repository: ClassNodeRepository,
) {

    fun findAllByDecompositionJobId(decompositionJobId: UUID) =
        repository.findAllByDecompositionJobId(decompositionJobId)
}
