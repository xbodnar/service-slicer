package cz.bodnor.serviceslicer.application.module.graph

import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.graph.service.BuildDependencyGraph
import cz.bodnor.serviceslicer.application.module.graph.service.CollectCompilationUnits
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class BuildDependencyGraphCommandHandler(
    private val projectFinderService: ProjectFinderService,
    private val classNodeCreateService: ClassNodeCreateService,
    private val collectCompilationUnits: CollectCompilationUnits,
    private val buildDependencyGraph: BuildDependencyGraph,
) : CommandHandler<Unit, BuildDependencyGraphCommand> {
    override val command = BuildDependencyGraphCommand::class

    override fun handle(command: BuildDependencyGraphCommand) {
        val project = projectFinderService.getById(command.projectId)
        require(project.javaProjectRoot != null) { "Java project root dir is missing" }

        val graphNodes = buildDependencyGraph(projectId = project.id, javaProjectRootDir = project.javaProjectRoot!!)

        classNodeCreateService.save(graphNodes.values.toList())
    }
}
