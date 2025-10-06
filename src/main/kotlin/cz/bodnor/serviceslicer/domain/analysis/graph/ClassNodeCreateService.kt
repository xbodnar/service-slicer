package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClassNodeCreateService(
    private val repository: ClassNodeRepository,
) {

    @Transactional
    fun save(classNodes: List<ClassNode>) {
        // Save nodes with relationships in a single transaction
        // Spring Data Neo4j will automatically cascade and save relationships
        repository.saveAll(classNodes)
    }

    @Transactional
    fun saveGraphWithRelationships(classNodesMap: Map<String, ClassNode>) {
        // Phase 1: Save all nodes without relationships to get IDs assigned
        val nodesWithoutRelationships = classNodesMap.values.map { node ->
            node.copy().also { it.dependencies = emptyList() }
        }
        val savedNodes = repository.saveAll(nodesWithoutRelationships)

        // Create a map from FQN to saved node (with ID)
        val fqnToSavedNode = savedNodes.associateBy { it.fullyQualifiedClassName }

        // Phase 2: Build and save relationships using saved nodes
        classNodesMap.forEach { (fqn, originalNode) ->
            val savedNode = fqnToSavedNode[fqn] ?: return@forEach

            // Build dependencies using saved nodes (which have IDs)
            savedNode.dependencies = originalNode.dependencies.mapNotNull { dep ->
                fqnToSavedNode[dep.target.fullyQualifiedClassName]?.let { savedTarget ->
                    ClassNodeDependency(
                        target = savedTarget,
                        weight = dep.weight,
                        methodCalls = dep.methodCalls,
                        fieldAccesses = dep.fieldAccesses,
                        objectCreations = dep.objectCreations,
                        typeReferences = dep.typeReferences,
                    )
                }
            }
        }

        // Save all nodes again with relationships
        repository.saveAll(savedNodes)
    }
}
