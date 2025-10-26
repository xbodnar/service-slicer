package cz.bodnor.serviceslicer.application.module.analysis.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class RunAnalysisJobCommand(
    val projectId: UUID,
) : Command<RunAnalysisJobCommand.Result> {

    data class Result(
        val analysisJobId: UUID,
    )
}

data class BuildDependencyGraphCommand(
    val projectId: UUID,
) : Command<Unit>

data class SuggestMicroserviceBoundariesCommand(
    val projectId: UUID,
) : Command<Unit>
