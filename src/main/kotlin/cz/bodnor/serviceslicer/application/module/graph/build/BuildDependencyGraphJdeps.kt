package cz.bodnor.serviceslicer.application.module.graph.build

import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadFileFromStorage
import cz.bodnor.serviceslicer.application.module.graph.service.DotGraphParser
import cz.bodnor.serviceslicer.application.module.graph.service.JdepsService
import cz.bodnor.serviceslicer.application.module.project.service.ProjectReadService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeType
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceReadService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BuildDependencyGraphJdeps(
    private val projectReadService: ProjectReadService,
    private val projectSourceReadService: ProjectSourceReadService,
    private val fileReadService: FileReadService,
    private val downloadFileFromStorage: DownloadFileFromStorage,
    private val jdepsService: JdepsService,
) : BuildDependencyGraph {

    override fun invoke(projectId: UUID): BuildDependencyGraph.Graph {
        val project = projectReadService.getById(projectId)
        val projectSource = projectSourceReadService.getById(project.projectSourceId)
        val jarFile = fileReadService.getById(projectSource.jarFileId)

        // TODO: Donwload JAR file to tmp dir
        val jarFilePath = downloadFileFromStorage(jarFile.storageKey, ".jar")

        // Run jdeps on the JAR file and get the output .dot file
        val dotFile = jdepsService.execute(
            jarFile = jarFilePath,
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
}
