package cz.bodnor.serviceslicer.domain.decomposition

import org.springframework.stereotype.Service

@Service
class DecompositionJobWriteService(
    private val repository: DecompositionJobRepository,
) {

    fun save(decompositionJob: DecompositionJob) = repository.save(decompositionJob)
}
