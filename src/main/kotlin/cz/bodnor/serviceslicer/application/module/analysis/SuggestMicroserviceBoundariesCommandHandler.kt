package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.SuggestMicroserviceBoundariesCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.CommunityDetectionBoundaryDetector
import cz.bodnor.serviceslicer.application.module.analysis.service.GraphAnalysisService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.analysis.suggestion.MicroserviceSuggestionCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SuggestMicroserviceBoundariesCommandHandler(
    private val projectFinderService: ProjectFinderService,
    private val graphAnalysisService: GraphAnalysisService,
    private val communityDetectionDetector: CommunityDetectionBoundaryDetector,
    private val suggestionCreateService: MicroserviceSuggestionCreateService,
) : CommandHandler<Unit, SuggestMicroserviceBoundariesCommand> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val command = SuggestMicroserviceBoundariesCommand::class

    override fun handle(command: SuggestMicroserviceBoundariesCommand) {
        val project = projectFinderService.getById(command.projectId)

        logger.info("Generating microservice boundary suggestions for project ${project.id}")

        // Load all class nodes with their references from Neo4j
        val classNodes = graphAnalysisService.getClassNodesWithReferences(project.id)

        if (classNodes.isEmpty()) {
            logger.warn("No class nodes found for project ${project.id}. Skipping suggestion generation.")
            return
        }

        logger.info("Loaded ${classNodes.size} class nodes for project ${project.id}")

        // Run community detection
        logger.info("Running community detection boundary detection")
        val communityDetectionSuggestion = communityDetectionDetector(
            analysisJobId = project.id,
            classNodes = classNodes,
        )
        suggestionCreateService.save(communityDetectionSuggestion)
        logger.info(
            "Community detection created ${communityDetectionSuggestion.boundaries.size} boundaries " +
                "with modularity score ${communityDetectionSuggestion.modularityScore}",
        )

        logger.info("Successfully generated microservice boundary suggestions for project ${project.id}")
    }
}
