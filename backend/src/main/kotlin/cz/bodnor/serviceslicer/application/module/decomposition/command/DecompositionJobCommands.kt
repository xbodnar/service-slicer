package cz.bodnor.serviceslicer.application.module.decomposition.command

import cz.bodnor.serviceslicer.application.module.decomposition.service.CommunityDetectionAlgorithm
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateDecompositionJobCommand(
    val name: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val jarFileId: UUID,
) : Command<DecompositionJob>

data class BuildDependencyGraphCommand(
    val decompositionJobId: UUID,
) : Command<Unit>

data class DetectGraphCommunitiesCommand(
    val decompositionJobId: UUID,
    val algorithm: CommunityDetectionAlgorithm,
) : Command<Unit>

data class DomainExpertDecompositionCommand(
    val decompositionJobId: UUID,
    val decompositionType: DomainDecompositionType,
) : Command<Unit> {

    enum class DomainDecompositionType {
        DOMAIN_DRIVEN,
        ACTOR_DRIVEN,
    }
}

data class RestartDecompositionJobCommand(
    val decompositionJobId: UUID,
) : Command<DecompositionJob>
