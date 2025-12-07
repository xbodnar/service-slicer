package cz.bodnor.serviceslicer.application.module.decomposition

import cz.bodnor.serviceslicer.adapter.out.jdeps.BuildDependencyGraphJdeps
import cz.bodnor.serviceslicer.application.module.decomposition.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.domain.graph.ClassNodeWriteService
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class BuildDependencyGraphCommandHandler(
    private val classNodeWriteService: ClassNodeWriteService,
    private val buildDependencyGraphJdeps: BuildDependencyGraphJdeps,
) : CommandHandler<Unit, BuildDependencyGraphCommand> {

    private val logger = logger()

    override val command = BuildDependencyGraphCommand::class

    override fun handle(command: BuildDependencyGraphCommand) {
        logger.info { "Building dependency graph for decompositionJob: ${command.decompositionJobId}" }

        val graph = buildDependencyGraphJdeps(decompositionJobId = command.decompositionJobId)

        classNodeWriteService.replaceGraph(command.decompositionJobId, graph.nodes)

        logger.info { "Successfully created graph for decompositionJob: ${command.decompositionJobId}" }
    }
}
