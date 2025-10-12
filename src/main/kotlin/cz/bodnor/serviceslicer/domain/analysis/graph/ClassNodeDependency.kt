package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.RelationshipId
import org.springframework.data.neo4j.core.schema.RelationshipProperties
import org.springframework.data.neo4j.core.schema.TargetNode

@RelationshipProperties
data class ClassNodeDependency(
    @Id @GeneratedValue
    val id: String? = null,

    @TargetNode
    val target: ClassNode,

    val weight: Int,

    val methodCalls: Int = 0,

    val fieldAccesses: Int = 0,

    val objectCreations: Int = 0,

    val typeReferences: Int = 0,
)
