package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.DomainExpertDecompositionCommand
import cz.bodnor.serviceslicer.application.module.analysis.port.out.DomainDecomposition
import cz.bodnor.serviceslicer.application.module.analysis.service.CalculateBoundaryMetrics
import cz.bodnor.serviceslicer.domain.analysis.decomposition.DecompositionApproach
import cz.bodnor.serviceslicer.domain.analysis.decomposition.MonolithDecomposition
import cz.bodnor.serviceslicer.domain.analysis.decomposition.MonolithDecompositionWriteService
import cz.bodnor.serviceslicer.domain.analysis.decomposition.ServiceBoundary
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeReadService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DomainExpertDecompositionCommandHandler(
    private val classNodeReadService: ClassNodeReadService,
    private val classNodeWriteService: ClassNodeWriteService,
    private val decompositionWriteService: MonolithDecompositionWriteService,
    private val domainDecomposition: DomainDecomposition,
    private val calculateBoundaryMetrics: CalculateBoundaryMetrics,
) : CommandHandler<Unit, DomainExpertDecompositionCommand> {

    private val logger = KotlinLogging.logger {}

    override val command = DomainExpertDecompositionCommand::class

    override fun handle(command: DomainExpertDecompositionCommand) {
        logger.info { "Starting domain-driven decomposition for project: ${command.projectId}" }

        // Fetch all class nodes for the project
        val classNodes = classNodeReadService.findAllByProjectId(command.projectId)
        require(classNodes.isNotEmpty()) { "No class nodes found for project ${command.projectId}" }

        with(command.decompositionType) {
            val decomposition = applyDecomposition(classNodes)
            updateNodeClusterIds(decomposition, classNodes)
            saveDecomposition(command.projectId, decomposition, classNodes)
        }

        logger.info { "Successfully updated domain-driven cluster IDs for project: ${command.projectId}" }
    }

    private fun DomainDecompositionType.updateNodeClusterIds(
        decomposition: DomainDecomposition.Result,
        classNodes: List<ClassNode>,
    ) {
        val classNodesByName = classNodes.associateBy { it.fullyQualifiedClassName }

        decomposition.microservices.forEach { microservice ->
            microservice.classes.forEach { className ->
                val node = classNodesByName[className]

                node?.let { node ->
                    when (this) {
                        DomainDecompositionType.DOMAIN_DRIVEN -> node.domainDrivenClusterId = microservice.clusterId
                        DomainDecompositionType.ACTOR_DRIVEN -> node.actorDrivenClusterId = microservice.clusterId
                    }
                    classNodeWriteService.update(node)
                }
                    ?: logger.warn { "Class $className not found in project" }
            }
        }
    }

    private fun DomainDecompositionType.saveDecomposition(
        projectId: UUID,
        decomposition: DomainDecomposition.Result,
        classNodes: List<ClassNode>,
    ) {
        val decompositionId = UUID.randomUUID()

        decompositionWriteService.save(
            decomposition = MonolithDecomposition(
                projectId = projectId,
                id = decompositionId,
                algorithm = when (this) {
                    DomainDecompositionType.DOMAIN_DRIVEN -> DecompositionApproach.DOMAIN_DRIVEN_DECOMPOSITION
                    DomainDecompositionType.ACTOR_DRIVEN -> DecompositionApproach.ACTOR_DRIVEN_DECOMPOSITION
                },
                modularityScore = 0.0, // TODO: Calculate modularity score
            ),
            serviceBoundaries = decomposition.microservices.map { microservice ->
                ServiceBoundary(
                    monolithDecompositionId = decompositionId,
                    suggestedName = microservice.clusterName,
                    metrics = calculateBoundaryMetrics(
                        classNodes.filter {
                            it.fullyQualifiedClassName in
                                microservice.classes
                        },
                        classNodes,
                    ),
                    typeNames = microservice.classes,
                )
            },
        )
    }

    private fun DomainDecompositionType.applyDecomposition(classNodes: List<ClassNode>): DomainDecomposition.Result =
        when (this) {
            DomainDecompositionType.DOMAIN_DRIVEN -> domainDecomposition.domainDriven(classNodes)
            DomainDecompositionType.ACTOR_DRIVEN -> domainDecomposition.actorDriven(classNodes)
        }
}

enum class DomainDecompositionType {
    DOMAIN_DRIVEN,
    ACTOR_DRIVEN,
}
