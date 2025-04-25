package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.analysis.graph.BuildDependencyGraph
import cz.bodnor.serviceslicer.application.module.analysis.graph.CollectCompilationUnits
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.analysis.graph.TypeNodeCreateService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class BuildDependencyGraphCommandHandler(
    private val projectFinderService: ProjectFinderService,
    private val typeNodeCreateService: TypeNodeCreateService,
    private val collectCompilationUnits: CollectCompilationUnits,
    private val buildDependencyGraph: BuildDependencyGraph,
) : CommandHandler<Unit, BuildDependencyGraphCommand> {
    override val command = BuildDependencyGraphCommand::class

    override fun handle(command: BuildDependencyGraphCommand) {
        val project = projectFinderService.getById(command.projectId)
        require(project.projectRoot != null) { "Project root dir is missing" }

        val graphNodes = buildDependencyGraph(projectId = project.id, projectRootDir = project.projectRoot!!)

        typeNodeCreateService.save(graphNodes.values.toList())
    }
}
