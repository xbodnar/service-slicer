package cz.bodnor.serviceslicer.application.module.graph

import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.graph.build.BuildDependencyGraph
import cz.bodnor.serviceslicer.application.module.graph.service.CollectCompilationUnits
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.application.module.projectsource.ProjectSourceFinderService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeCreateService
import cz.bodnor.serviceslicer.domain.projectsource.SourceType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class BuildDependencyGraphCommandHandler(
    private val projectFinderService: ProjectFinderService,
    private val projectSourceFinderService: ProjectSourceFinderService,
    private val classNodeCreateService: ClassNodeCreateService,
    private val collectCompilationUnits: CollectCompilationUnits,
    dependencyGraphBuilders: List<BuildDependencyGraph>,
) : CommandHandler<Unit, BuildDependencyGraphCommand> {

    private val graphBuilders = SourceType.entries.associateWith { sourceType ->
        val buildersMatchingSourceType = dependencyGraphBuilders.filter {
            it.supportedSourceTypes().contains(sourceType)
        }
        require(buildersMatchingSourceType.size == 1) {
            "Expected exactly one dependency graph builder for source type $sourceType, " +
                "found ${buildersMatchingSourceType.size}: $buildersMatchingSourceType"
        }

        buildersMatchingSourceType.first()
    }

    private val logger = logger()

    override val command = BuildDependencyGraphCommand::class

    override fun handle(command: BuildDependencyGraphCommand) {
        logger.info("Building dependency graph for project: ${command.projectId}")
        val projectSource = projectSourceFinderService.getByProjectId(command.projectId)

        val graphBuilder = graphBuilders[projectSource.sourceType]!!

        val graph = graphBuilder(projectId = command.projectId)

        classNodeCreateService.replaceGraph(command.projectId, graph.nodes)

        logger.info("Successfully created graph for project: ${command.projectId}")
    }
}
