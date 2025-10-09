package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.data.neo4j.core.Neo4jClient
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClassNodeCreateService(
    private val repository: ClassNodeRepository,
    private val neo4jTemplate: Neo4jTemplate,
    private val neo4jClient: Neo4jClient,
) {

    @Transactional
    fun save(classNodes: List<ClassNode>) {
        // Save nodes with relationships in a single transaction
        // Spring Data Neo4j will automatically cascade and save relationships
        repository.saveAll(classNodes)
    }
}
