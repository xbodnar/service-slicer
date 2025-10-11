package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Node
class ClassNode(

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
    var dependencies: MutableList<ClassNodeDependency> = mutableListOf()

    fun addDependency(
        target: ClassNode,
        weight: Int,
        methodCalls: Int = 0,
        fieldAccesses: Int = 0,
        objectCreations: Int = 0,
        typeReferences: Int = 0,
    ) {
        val dependency = ClassNodeDependency(
            target = target,
            weight = weight,
            methodCalls = methodCalls,
            fieldAccesses = fieldAccesses,
            objectCreations = objectCreations,
            typeReferences = typeReferences,
        )

        dependencies.add(dependency)
    }
}

enum class ClassNodeType {
    CLASS,
    INTERFACE,
    ENUM,
}

@Repository
interface ClassNodeRepository : Neo4jRepository<ClassNode, String> {

    fun findAllByProjectId(projectId: UUID): List<ClassNode>
}
