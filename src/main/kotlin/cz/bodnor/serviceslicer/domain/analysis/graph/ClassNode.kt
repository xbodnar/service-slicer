package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Node
data class ClassNode(

    @Id
    @GeneratedValue
    val id: String? = null,

    val type: ClassNodeType,

    val simpleClassName: String,

    val fullyQualifiedClassName: String,

    val projectId: UUID,
) {

    @Relationship(
        type = "DEPENDS_ON",
        direction = Relationship.Direction.OUTGOING,
    )
    var dependencies: List<ClassNodeDependency> = emptyList()
}

enum class ClassNodeType {
    CLASS,
    INTERFACE,
    ENUM,
}

@Repository
interface ClassNodeRepository : Neo4jRepository<ClassNode, String>
