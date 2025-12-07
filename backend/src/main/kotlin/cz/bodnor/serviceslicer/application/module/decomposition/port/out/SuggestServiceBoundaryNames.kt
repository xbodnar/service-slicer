package cz.bodnor.serviceslicer.application.module.decomposition.port.out

import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.CommunityDetectionStrategy
import java.util.UUID

interface SuggestServiceBoundaryNames {

    data class Result(
        val serviceNames: List<ServiceNameSuggestion>,
    ) {
        data class ServiceNameSuggestion(
            val id: UUID,
            val serviceName: String,
        )
    }

    operator fun invoke(serviceBoundaries: List<CommunityDetectionStrategy.Result.Community>): Result
}
