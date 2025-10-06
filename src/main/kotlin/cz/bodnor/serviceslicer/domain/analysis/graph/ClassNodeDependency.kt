package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.RelationshipId
import org.springframework.data.neo4j.core.schema.RelationshipProperties
import org.springframework.data.neo4j.core.schema.TargetNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

@RelationshipProperties
data class ClassNodeDependency(
    @RelationshipId
    @GeneratedValue
    val id: String? = null,

    @TargetNode
    val target: ClassNode,

    val weight: Int,

    val methodCalls: Int = 0,

    val fieldAccesses: Int = 0,

    val objectCreations: Int = 0,

    val typeReferences: Int = 0,
)

@Repository
interface ClassNodeDependencyRepository : Neo4jRepository<ClassNodeDependency, String>
