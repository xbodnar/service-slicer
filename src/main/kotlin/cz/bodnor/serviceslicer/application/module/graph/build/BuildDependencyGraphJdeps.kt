package cz.bodnor.serviceslicer.application.module.graph.build

import cz.bodnor.serviceslicer.application.module.graph.service.DotGraphParser
import cz.bodnor.serviceslicer.application.module.graph.service.JdepsService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.application.module.projectsource.ProjectSourceFinderService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeType
import cz.bodnor.serviceslicer.domain.projectsource.JarProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.SourceType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BuildDependencyGraphJdeps(
    private val projectFinderService: ProjectFinderService,
    private val projectSourceFinderService: ProjectSourceFinderService,
    private val jdepsService: JdepsService,
) : BuildDependencyGraph {

    override fun invoke(projectId: UUID): BuildDependencyGraph.Graph {
        val project = projectFinderService.getById(projectId)
        val projectSource = projectSourceFinderService.getByProjectId(projectId)
        require(projectSource is JarProjectSource) { "Only JAR projects are supported for jdeps analysis" }

        // Run jdeps on the JAR file and get the output .dot file
        val dotFile = jdepsService.execute(
            jarFile = projectSource.jarFilePath,
            basePackageName = project.basePackageName,
            excludePackages = project.excludePackages,
        )

        val graph = DotGraphParser.parse(dotFile)

        val nodes = graph.vertices.map {
            ClassNode(
                type = ClassNodeType.UNKNOWN,
                simpleClassName = it.split('.').last(),
                fullyQualifiedClassName = it,
                projectId = projectId,
            )
        }.associateBy { it.fullyQualifiedClassName }

        graph.edges.forEach { (source, target) ->
            nodes[source]!!.addDependency(
                target = nodes[target]!!,
                weight = 1,
            )
        }

        return BuildDependencyGraph.Graph(
            nodes = nodes.values.toList(),
        )
    }

    override fun supportedSourceTypes(): Set<SourceType> = setOf(SourceType.JAR)
}
