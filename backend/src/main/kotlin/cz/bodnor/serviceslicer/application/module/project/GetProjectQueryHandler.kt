package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.query.GetProjectQuery
import cz.bodnor.serviceslicer.application.module.project.service.ProjectReadService
import cz.bodnor.serviceslicer.domain.analysis.decomposition.MonolithDecompositionReadService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetProjectQueryHandler(
    private val projectReadService: ProjectReadService,
    private val decompositionReadService: MonolithDecompositionReadService,
    private val classNodeRepository: ClassNodeRepository,
) : QueryHandler<GetProjectQuery.Result, GetProjectQuery> {
    override val query = GetProjectQuery::class

    override fun handle(query: GetProjectQuery): GetProjectQuery.Result {
        val project = projectReadService.getById(query.projectId)

        val graphNodes = classNodeRepository.findAllByProjectId(query.projectId)

        val decompositions = decompositionReadService.findAllByProjectId(query.projectId)

        // TODO: Add metadata from ServiceBoundary entities
        return GetProjectQuery.Result(
            projectId = project.id,
            name = project.name,
            analysisJobResult = GetProjectQuery.AnalysisJobResult(
                staticAnalysis = GetProjectQuery.StaticAnalysisResult(
                    dependencyGraph = GetProjectQuery.GraphSummary(
                        nodeCount = graphNodes.size,
                        edgeCount = graphNodes.sumOf { it.dependencies.size },
                        nodes = graphNodes.map { node ->
                            GetProjectQuery.ClassNodeDto(
                                simpleClassName = node.simpleClassName,
                                fullyQualifiedClassName = node.fullyQualifiedClassName,
                                dependencies = node.dependencies.map { it.target.fullyQualifiedClassName },
                            )
                        },
                    ),
                    labelPropagationAlgorithm = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.communityLabelPropagation.toString() },
                    ),
                    louvainAlgorithm = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.communityLouvain.toString() },
                    ),
                    leidenAlgorithm = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.communityLeiden.toString() },
                    ),
                    domainDrivenDecomposition = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.domainDrivenClusterId },
                    ),
                    actorDrivenDecomposition = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.actorDrivenClusterId },
                    ),
                ),
            ),
        )
    }

    private fun List<ClassNode>.communities(keySelector: (ClassNode) -> String?): Map<String, List<String>> =
        this.filter { node -> keySelector(node) != null }
            .groupBy { keySelector(it)!! }
            .mapValues { it.value.map { node -> node.fullyQualifiedClassName } }
            .toMap()
}
