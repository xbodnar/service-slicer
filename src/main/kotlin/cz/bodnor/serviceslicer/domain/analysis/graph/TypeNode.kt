package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Node
data class TypeNode(

    @Id
    @GeneratedValue
    val id: UUID = UUID.randomUUID(),

    val type: TypeNodeType,

    val simpleClassName: String,

    val fullyQualifiedClassName: String,

    val projectId: UUID,
) {

    @Relationship(type = "REFERENCES", direction = Relationship.Direction.OUTGOING)
    var references: List<TypeNode> = emptyList()
}

enum class TypeNodeType {
    CLASS,
    INTERFACE,
    ENUM,
}

@Repository
interface TypeNodeRepository : Neo4jRepository<TypeNode, UUID>
