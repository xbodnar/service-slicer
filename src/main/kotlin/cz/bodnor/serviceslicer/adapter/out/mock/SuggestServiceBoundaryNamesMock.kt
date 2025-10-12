package cz.bodnor.serviceslicer.adapter.out.mock

import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.CommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.port.out.SuggestServiceBoundaryNames
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("integration.service-boundary-name-suggestor.mock", havingValue = "true")
class SuggestServiceBoundaryNamesMock : SuggestServiceBoundaryNames {
    override fun invoke(
        serviceBoundaries: List<CommunityDetectionStrategy.Result.Community>,
    ): SuggestServiceBoundaryNames.Result = SuggestServiceBoundaryNames.Result(
        serviceNames = serviceBoundaries.map {
            SuggestServiceBoundaryNames.Result.ServiceNameSuggestion(
                id = it.id,
                serviceName = "ServiceNameMock${it.id.toString().take(4)}",
            )
        },
    )
}
