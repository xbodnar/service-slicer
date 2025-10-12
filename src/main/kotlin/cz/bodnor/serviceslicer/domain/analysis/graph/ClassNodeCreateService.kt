package cz.bodnor.serviceslicer.domain.analysis.graph

import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ClassNodeCreateService(
    private val repository: ClassNodeRepository,
    private val neo4jTemplate: Neo4jTemplate,
    private val neo4jClient: Neo4jClient,
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
}
