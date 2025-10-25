package cz.bodnor.serviceslicer.application.module.graph.build

import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BuildDependencyGraphJdeps(
    private val projectFinderService: ProjectFinderService,
) : BuildDependencyGraph {

    override fun invoke(projectId: UUID): BuildDependencyGraph.Graph {
        val project = projectFinderService.getById(projectId)

        TODO()
    }
}
