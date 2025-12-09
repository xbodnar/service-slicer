package cz.bodnor.serviceslicer.adapter.out.mock

import cz.bodnor.serviceslicer.application.module.decomposition.port.out.SuggestServiceBoundaryNames
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty("integration.service-boundary-name-suggester.mock", havingValue = "true")
class SuggestServiceBoundaryNamesMock : SuggestServiceBoundaryNames {

    override fun invoke(
        services: List<SuggestServiceBoundaryNames.ServiceCluster>,
    ): SuggestServiceBoundaryNames.Result = SuggestServiceBoundaryNames.Result(
        services.associate {
            it.id to "ServiceNameMock${it.id.toString().take(4)}"
        },
    )
}
