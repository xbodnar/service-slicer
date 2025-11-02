package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.analysis.service.AnalysisJobFinderService
import cz.bodnor.serviceslicer.application.module.project.query.GetProjectQuery
import cz.bodnor.serviceslicer.application.module.project.service.ProjectReadService
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetProjectQueryHandler(
    private val projectReadService: ProjectReadService,
    private val analysisJobFinderService: AnalysisJobFinderService,
    private val classNodeRepository: ClassNodeRepository,
) : QueryHandler<GetProjectQuery.Result, GetProjectQuery> {
    override val query = GetProjectQuery::class

    override fun handle(query: GetProjectQuery): GetProjectQuery.Result {
        val project = projectReadService.getById(query.projectId)

        val analysisJob = analysisJobFinderService.getByProjectId(query.projectId)

        val graphNodes = classNodeRepository.findAllByProjectId(query.projectId)

        return GetProjectQuery.Result(
            projectId = project.id,
            name = project.name,
            analysisJobResult = GetProjectQuery.AnalysisJobResult(
                analysisJobId = analysisJob.id,
                status = analysisJob.status,
                staticAnalysis = GetProjectQuery.StaticAnalysisResult(
                    dependencyGraph = GetProjectQuery.GraphSummary(
                        nodeCount = graphNodes.size,
                        edgeCount = graphNodes.sumOf { it.dependencies.size },
                    ),
                    labelPropagationAlgorithm = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.communityLabelPropagation },
                    ),
                    louvainAlgorithm = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.communityLouvain },
                    ),
                    leidenAlgorithm = GetProjectQuery.DecompositionResults(
                        communities = graphNodes.communities { it.communityLeiden },
                    ),
                ),
            ),
        )
    }

    private fun List<ClassNode>.communities(keySelector: (ClassNode) -> Long?): Map<Long, List<String>> =
        this.takeIf { nodes -> nodes.all { keySelector(it) != null } }
            ?.groupBy { keySelector(it)!! }
            ?.mapValues { it.value.map { node -> node.fullyQualifiedClassName } }
            ?.toMap() ?: emptyMap()
}
