package cz.bodnor.serviceslicer.application.module.analysis.port.out

import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode

interface DomainDecomposition {

    data class MicroserviceCluster(
        val clusterId: String,
        val clusterName: String,
        val description: String,
        val classes: List<String>,
    )

    data class Result(
        val microservices: List<MicroserviceCluster>,
    )

    fun domainDriven(graph: List<ClassNode>): Result

    fun actorDriven(graph: List<ClassNode>): Result
}
