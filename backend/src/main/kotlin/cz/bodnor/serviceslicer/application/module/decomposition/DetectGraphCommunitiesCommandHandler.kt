package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.command.DetectGraphCommunitiesCommand
import cz.bodnor.serviceslicer.application.module.decomposition.port.out.SuggestServiceBoundaryNames
import cz.bodnor.serviceslicer.application.module.decomposition.service.CalculateBoundaryMetrics
import cz.bodnor.serviceslicer.application.module.decomposition.service.CommunityDetectionAlgorithm
import cz.bodnor.serviceslicer.application.module.decomposition.service.RunCommunityDetectionAlgorithm
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidate
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidateWriteService
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionMethod
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import cz.bodnor.serviceslicer.domain.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DetectGraphCommunitiesCommandHandler(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val decompositionCandidateWriteService: DecompositionCandidateWriteService,
    private val classNodeRepository: ClassNodeRepository,
    private val runCommunityDetectionAlgorithm: RunCommunityDetectionAlgorithm,
    private val suggestServiceBoundaryNames: SuggestServiceBoundaryNames,
    private val calculateBoundaryMetrics: CalculateBoundaryMetrics,
) : CommandHandler<Unit, DetectGraphCommunitiesCommand> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val command = DetectGraphCommunitiesCommand::class

    @Lazy
    @Autowired
    private lateinit var self: DetectGraphCommunitiesCommandHandler

    override fun handle(command: DetectGraphCommunitiesCommand) {
        require(decompositionJobReadService.existsById(command.decompositionJobId)) {
            "DecompositionJob with ID: ${command.decompositionJobId} not found"
        }

        logger.info(
            "Running community detection ${command.algorithm} for DecompositionJob ${command.decompositionJobId}",
        )

        val result = runCommunityDetectionAlgorithm(command.decompositionJobId, command.algorithm)
        val graph = classNodeRepository.findAllByDecompositionJobId(command.decompositionJobId)

        val serviceNameSuggestions = getServiceNameSuggestions(command.decompositionJobId, command.algorithm, graph)

        self.saveDecompositionCandidate(
            command.decompositionJobId,
            command.algorithm,
            result,
            serviceNameSuggestions,
            graph,
        )

        logger.info(
            "Successfully generated decompositionCandidate DecompositionJob ${command.decompositionJobId}",
        )
    }

    private fun getServiceNameSuggestions(
        decompositionJobId: UUID,
        algorithm: CommunityDetectionAlgorithm,
        graph: List<ClassNode>,
    ): Map<String, String> {
        val projectGraph = classNodeRepository.findAllByDecompositionJobId(decompositionJobId)
        val services = projectGraph.groupBy {
            when (algorithm) {
                CommunityDetectionAlgorithm.LEIDEN -> it.communityLeiden
                CommunityDetectionAlgorithm.LOUVAIN -> it.communityLouvain
                CommunityDetectionAlgorithm.LABEL_PROPAGATION -> it.communityLabelPropagation
            }
        }

        return suggestServiceBoundaryNames(
            services = services.map { (serviceId, nodes) ->
                SuggestServiceBoundaryNames.ServiceCluster(
                    serviceId.toString(),
                    nodes.map { it.fullyQualifiedClassName },
                )
            },
        ).serviceNameSuggestions
    }

    @Transactional
    fun saveDecompositionCandidate(
        decompositionJobId: UUID,
        algorithm: CommunityDetectionAlgorithm,
        result: RunCommunityDetectionAlgorithm.Result,
        serviceNameSuggestions: Map<String, String>,
        graph: List<ClassNode>,
    ) {
        val decompositionJob = decompositionJobReadService.getById(decompositionJobId)

        val decompositionCandidate = DecompositionCandidate(
            decompositionJob = decompositionJob,
            method = algorithm.toDecompositionMethod(),
            modularity = result.modularity?.toBigDecimal(),
        )

        result.communities.forEach { (communityId, nodes) ->
            decompositionCandidate.addServiceBoundary(
                name = serviceNameSuggestions[communityId] ?: "Cluster-$communityId",
                metrics = calculateBoundaryMetrics(nodes, graph),
                typeNames = nodes.map { it.fullyQualifiedClassName },
            )
        }

        decompositionCandidateWriteService.save(decompositionCandidate)
    }

    private fun CommunityDetectionAlgorithm.toDecompositionMethod(): DecompositionMethod = when (this) {
        CommunityDetectionAlgorithm.LEIDEN -> DecompositionMethod.COMMUNITY_DETECTION_LEIDEN
        CommunityDetectionAlgorithm.LOUVAIN -> DecompositionMethod.COMMUNITY_DETECTION_LOUVAIN
        CommunityDetectionAlgorithm.LABEL_PROPAGATION -> DecompositionMethod.COMMUNITY_DETECTION_LABEL_PROPAGATION
    }
}
