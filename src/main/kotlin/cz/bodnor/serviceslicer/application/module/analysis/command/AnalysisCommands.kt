package cz.bodnor.serviceslicer.application.module.analysis.command

import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateAnalysisJobCommand(
    val projectId: UUID,
    val analysisType: AnalysisType,
) : Command<CreateAnalysisJobCommand.Result> {

    data class Result(
        val analysisJobId: UUID,
    )
}

data class RunAnalysisJobCommand(
    val analysisJobId: UUID,
) : Command<Unit>
