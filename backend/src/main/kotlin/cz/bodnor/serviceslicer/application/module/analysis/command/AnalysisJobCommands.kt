package cz.bodnor.serviceslicer.application.module.analysis.command

import cz.bodnor.serviceslicer.application.module.analysis.DomainDecompositionType
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class RunJobCommand(
    val projectId: UUID,
    val jobType: JobType,
) : Command<RunJobCommand.Result> {

    data class Result(
        val projectId: UUID,
    )
}

data class BuildDependencyGraphCommand(
    val projectId: UUID,
) : Command<Unit>

data class DetectGraphCommunitiesCommand(
    val projectId: UUID,
) : Command<Unit>

data class DomainExpertDecompositionCommand(
    val projectId: UUID,
    val decompositionType: DomainDecompositionType,
) : Command<Unit>
