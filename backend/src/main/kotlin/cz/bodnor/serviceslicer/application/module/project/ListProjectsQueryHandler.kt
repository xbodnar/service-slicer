package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.query.ListProjectsQuery
import cz.bodnor.serviceslicer.application.module.project.service.ProjectReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class ListProjectsQueryHandler(
    private val projectReadService: ProjectReadService,
) : QueryHandler<ListProjectsQuery.Result, ListProjectsQuery> {
    override val query = ListProjectsQuery::class

    override fun handle(query: ListProjectsQuery): ListProjectsQuery.Result {
        val projects = projectReadService.findAll()

        return ListProjectsQuery.Result(
            projects = projects.map {
                ListProjectsQuery.ProjectSummary(
                    projectId = it.id,
                    name = it.name,
                    basePackageName = it.basePackageName,
                    createdAt = it.createdAt,
                )
            },
        )
    }
}
