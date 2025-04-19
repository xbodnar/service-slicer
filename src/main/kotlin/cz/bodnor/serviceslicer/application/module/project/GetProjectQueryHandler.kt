package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.application.module.project.query.GetProjectQuery
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetProjectQueryHandler(
    private val projectFinderService: ProjectFinderService,
) : QueryHandler<GetProjectQuery.Result, GetProjectQuery> {
    override val query = GetProjectQuery::class

    override fun handle(query: GetProjectQuery): GetProjectQuery.Result =
        projectFinderService.getById(query.projectId).let {
            GetProjectQuery.Result(
                projectId = it.id,
                name = it.name,
                status = it.status,
                sourceType = it.sourceType,
            )
        }
}
