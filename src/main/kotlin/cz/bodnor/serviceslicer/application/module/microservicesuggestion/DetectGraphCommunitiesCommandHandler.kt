package cz.bodnor.serviceslicer.application.module.microservicesuggestion

import cz.bodnor.serviceslicer.application.module.analysis.command.DetectGraphCommunitiesCommand
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.service.CommunityDetectionService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectReadService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DetectGraphCommunitiesCommandHandler(
    private val projectReadService: ProjectReadService,
    private val classNodeRepository: ClassNodeRepository,
    private val communityDetectionService: CommunityDetectionService,
) : CommandHandler<Unit, DetectGraphCommunitiesCommand> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val command = DetectGraphCommunitiesCommand::class

    override fun handle(command: DetectGraphCommunitiesCommand) {
        val project = projectReadService.getById(command.projectId)
        val projectGraph = classNodeRepository.findAllByProjectId(command.projectId)

        require(projectGraph.size > 10) { "Project must have at least 10 classes" }

        logger.info("Generating microservice boundary suggestions for project ${project.id}")

        val leidenResult = communityDetectionService.runLeiden(command.projectId)
        val louvainResult = communityDetectionService.runLouvain(command.projectId)
        val labelPropagationResult = communityDetectionService.runLabelPropagation(command.projectId)

        logger.info("Successfully generated microservice boundary suggestions for project ${project.id}")
    }
}
