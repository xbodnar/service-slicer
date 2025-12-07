package cz.bodnor.serviceslicer.application.module.analysis.port.out

import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand
import cz.bodnor.serviceslicer.domain.graph.ClassNode

interface ApplyDomainExpertDecomposition {

    data class MicroserviceCluster(
        val clusterId: String,
        val clusterName: String,
        val description: String,
        val classes: List<String>,
    )

    data class Result(
        val microservices: List<MicroserviceCluster>,
    )

    operator fun invoke(
        graph: List<ClassNode>,
        type: DomainExpertDecompositionCommand.DomainDecompositionType,
    ): Result
}
