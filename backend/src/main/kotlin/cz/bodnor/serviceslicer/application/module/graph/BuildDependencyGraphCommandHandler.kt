package cz.bodnor.serviceslicer.application.module.graph

import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.graph.build.BuildDependencyGraphJdeps
import cz.bodnor.serviceslicer.application.module.graph.service.CollectCompilationUnits
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeWriteService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class BuildDependencyGraphCommandHandler(
    private val classNodeWriteService: ClassNodeWriteService,
    private val collectCompilationUnits: CollectCompilationUnits,
    private val buildDependencyGraphJdeps: BuildDependencyGraphJdeps,
) : CommandHandler<Unit, BuildDependencyGraphCommand> {

    private val logger = logger()

    override val command = BuildDependencyGraphCommand::class

    override fun handle(command: BuildDependencyGraphCommand) {
        logger.info { "Building dependency graph for project: ${command.projectId}" }

        val graph = buildDependencyGraphJdeps(projectId = command.projectId)

        classNodeWriteService.replaceGraph(command.projectId, graph.nodes)

        logger.info { "Successfully created graph for project: ${command.projectId}" }
    }
}
