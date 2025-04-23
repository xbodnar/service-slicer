package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TypeNodeCreateService(
    private val repository: TypeNodeRepository,
) {

    @Transactional
    fun save(typeNodes: List<TypeNode>) {
        repository.saveAll(typeNodes)
    }
}
