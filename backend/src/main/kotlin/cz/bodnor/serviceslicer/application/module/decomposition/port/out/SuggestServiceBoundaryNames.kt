package cz.bodnor.serviceslicer.application.module.decomposition.port.out

interface SuggestServiceBoundaryNames {

    data class ServiceCluster(
        val id: String,
        val classes: List<String>,
    )

    data class Result(
        // Map of service ID to suggested service name
        val serviceNameSuggestions: Map<String, String>,
    )

    operator fun invoke(services: List<ServiceCluster>): Result
}
