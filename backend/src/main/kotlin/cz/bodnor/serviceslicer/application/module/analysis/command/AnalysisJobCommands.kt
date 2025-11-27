package cz.bodnor.serviceslicer.application.module.analysis.command

import cz.bodnor.serviceslicer.application.module.analysis.DomainDecompositionType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

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
