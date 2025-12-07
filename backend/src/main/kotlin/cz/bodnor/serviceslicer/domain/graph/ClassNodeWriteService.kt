package cz.bodnor.serviceslicer.domain.graph

import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ClassNodeWriteService(
    private val repository: ClassNodeRepository,
) {

    private val logger = logger()

    @Transactional
    fun create(classNodes: List<ClassNode>) {
        repository.saveAll(classNodes)
    }

    @Transactional
    fun replaceGraph(
        decompositionJobId: UUID,
        classNodes: List<ClassNode>,
    ) {
        repository.deleteAllByDecompositionJobId(decompositionJobId)

        this.create(classNodes)
    }

    @Transactional
    fun update(node: ClassNode) {
        repository.save(node)
    }
}
