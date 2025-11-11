package cz.bodnor.serviceslicer.domain.analysis.graph

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
        projectId: UUID,
        classNodes: List<ClassNode>,
    ) {
        repository.deleteAllByProjectId(projectId)

        this.create(classNodes)
    }

    @Transactional
    fun update(node: ClassNode) {
        repository.save(node)
    }
}
