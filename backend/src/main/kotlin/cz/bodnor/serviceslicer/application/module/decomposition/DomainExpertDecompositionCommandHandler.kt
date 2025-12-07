package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.analysis.port.out.ApplyDomainExpertDecomposition
import cz.bodnor.serviceslicer.application.module.analysis.service.CalculateBoundaryMetrics
import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand
import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand.DomainDecompositionType.ACTOR_DRIVEN
import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand.DomainDecompositionType.DOMAIN_DRIVEN
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidate
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidateWriteService
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionMethod
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import cz.bodnor.serviceslicer.domain.graph.ClassNodeReadService
import cz.bodnor.serviceslicer.domain.graph.ClassNodeWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DomainExpertDecompositionCommandHandler(
    private val classNodeReadService: ClassNodeReadService,
    private val classNodeWriteService: ClassNodeWriteService,
    private val applyDomainExpertDecomposition: ApplyDomainExpertDecomposition,
    private val decompositionJobReadService: DecompositionJobReadService,
    private val decompositionCandidateWriteService: DecompositionCandidateWriteService,
    private val calculateBoundaryMetrics: CalculateBoundaryMetrics,
) : CommandHandler<Unit, DomainExpertDecompositionCommand> {

    @Lazy
    @Autowired
    private lateinit var self: DomainExpertDecompositionCommandHandler

    private val logger = KotlinLogging.logger {}

    override val command = DomainExpertDecompositionCommand::class

    override fun handle(command: DomainExpertDecompositionCommand) {
        logger.info {
            "Starting ${command.decompositionType} decomposition for DecompositionJob: ${command.decompositionJobId}"
        }

        // Fetch all class nodes for the decompositionJob
        val classNodes = classNodeReadService.findAllByDecompositionJobId(command.decompositionJobId)
        require(classNodes.isNotEmpty()) { "No class nodes found for DecompositionJob ${command.decompositionJobId}" }

        val decomposition = applyDomainExpertDecomposition(graph = classNodes, type = command.decompositionType)
        self.updateNodeClusterIds(command.decompositionType, decomposition, classNodes)
        self.saveDecompositionCandidate(
            command.decompositionType,
            command.decompositionJobId,
            decomposition,
            classNodes,
        )

        logger.info {
            "Successfully updated ${command.decompositionType} cluster IDs for DecompositionJob: ${command.decompositionJobId}"
        }
    }

    @Transactional
    fun updateNodeClusterIds(
        decompositionType: DomainExpertDecompositionCommand.DomainDecompositionType,
        decomposition: ApplyDomainExpertDecomposition.Result,
        classNodes: List<ClassNode>,
    ) {
        val classNodesByName = classNodes.associateBy { it.fullyQualifiedClassName }

        decomposition.microservices.forEach { microservice ->
            microservice.classes.forEach { className ->
                val node = classNodesByName[className]

                node?.let { node ->
                    when (decompositionType) {
                        DOMAIN_DRIVEN ->
                            node.domainDrivenClusterId =
                                microservice.clusterId

                        ACTOR_DRIVEN ->
                            node.actorDrivenClusterId =
                                microservice.clusterId
                    }
                    classNodeWriteService.update(node)
                }
                    ?: logger.warn { "Class $className not found in DecompositionJob" }
            }
        }
    }

    @Transactional
    fun saveDecompositionCandidate(
        decompositionType: DomainExpertDecompositionCommand.DomainDecompositionType,
        decompositionJobId: UUID,
        decomposition: ApplyDomainExpertDecomposition.Result,
        classNodes: List<ClassNode>,
    ) {
        val decompositionJob = decompositionJobReadService.getById(decompositionJobId)
        val decompositionMethod = when (decompositionType) {
            DOMAIN_DRIVEN -> DecompositionMethod.DOMAIN_DRIVEN_DECOMPOSITION
            ACTOR_DRIVEN -> DecompositionMethod.ACTOR_DRIVEN_DECOMPOSITION
        }

        val decompositionCandidate = DecompositionCandidate(
            decompositionJob = decompositionJob,
            method = decompositionMethod,
        )

        decomposition.microservices.forEach { microservice ->
            decompositionCandidate.addServiceBoundary(
                name = microservice.clusterName,
                metrics = calculateBoundaryMetrics(
                    classNodes.filter {
                        it.fullyQualifiedClassName in
                            microservice.classes
                    },
                    classNodes,
                ),
                typeNames = microservice.classes,
            )
        }

        decompositionCandidateWriteService.save(decompositionCandidate)
    }
}
