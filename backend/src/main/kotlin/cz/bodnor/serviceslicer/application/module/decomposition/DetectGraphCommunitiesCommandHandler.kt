package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.command.DetectGraphCommunitiesCommand
import cz.bodnor.serviceslicer.application.module.decomposition.service.CommunityDetectionService
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DetectGraphCommunitiesCommandHandler(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val classNodeRepository: ClassNodeRepository,
    private val communityDetectionService: CommunityDetectionService,
) : CommandHandler<Unit, DetectGraphCommunitiesCommand> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val command = DetectGraphCommunitiesCommand::class

    override fun handle(command: DetectGraphCommunitiesCommand) {
        val decompositionJob = decompositionJobReadService.getById(command.decompositionJobId)
        val projectGraph = classNodeRepository.findAllByDecompositionJobId(command.decompositionJobId)

        logger.info("Generating microservice boundary suggestions for DecompositionJob ${command.decompositionJobId}")

        val leidenResult = communityDetectionService.runLeiden(command.decompositionJobId)
        val louvainResult = communityDetectionService.runLouvain(command.decompositionJobId)
        val labelPropagationResult = communityDetectionService.runLabelPropagation(command.decompositionJobId)

        logger.info(
            "Successfully generated microservice boundary suggestions for DecompositionJob ${command.decompositionJobId}",
        )
    }
}
