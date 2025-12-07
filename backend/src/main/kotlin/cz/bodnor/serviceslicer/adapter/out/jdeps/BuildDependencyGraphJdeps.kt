package cz.bodnor.serviceslicer.adapter.out.jdeps

import cz.bodnor.serviceslicer.adapter.out.jdeps.DotGraphParser
import cz.bodnor.serviceslicer.application.module.decomposition.port.out.BuildDependencyGraph
import cz.bodnor.serviceslicer.application.module.file.service.DiskOperations
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import cz.bodnor.serviceslicer.domain.graph.ClassNodeType
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.deleteIfExists

@Component
class BuildDependencyGraphJdeps(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val jdepsCommandExecutor: JdepsCommandExecutor,
    private val diskOperations: DiskOperations,
) : BuildDependencyGraph {

    override fun invoke(decompositionJobId: UUID): BuildDependencyGraph.Graph {
        val decompositionJob = decompositionJobReadService.getById(decompositionJobId)
        val monolithArtifact = decompositionJob.monolithArtifact
        var dotFile: Path? = null

        return diskOperations.withFile(monolithArtifact.jarFile.id) { artifactFile ->
            // Run jdeps on the JAR file and get the output .dot file
            try {
                dotFile = jdepsCommandExecutor.execute(
                    jarFile = artifactFile,
                    basePackageName = monolithArtifact.basePackageName,
                    excludePackages = monolithArtifact.excludePackages,
                )

                val graph = DotGraphParser.parse(dotFile)

                val nodes = graph.vertices.map { vertex ->
                    ClassNode(
                        type = ClassNodeType.UNKNOWN,
                        simpleClassName = vertex.split('.').last(),
                        fullyQualifiedClassName = vertex,
                        decompositionJobId = decompositionJobId,
                    )
                }.associateBy { it.fullyQualifiedClassName }

                graph.edges.forEach { (source, target) ->
                    nodes[source]!!.addDependency(
                        target = nodes[target]!!,
                        weight = 1,
                    )
                }

                BuildDependencyGraph.Graph(
                    nodes = nodes.values.toList(),
                )
            } finally {
                dotFile?.deleteIfExists()
            }
        }
    }
}
