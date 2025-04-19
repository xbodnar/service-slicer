package cz.bodnor.serviceslicer.application.module.project.query

import cz.bodnor.serviceslicer.domain.project.ProjectStatus
import cz.bodnor.serviceslicer.domain.project.SourceType
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import java.util.UUID

data class GetProjectQuery(val projectId: UUID) : Query<GetProjectQuery.Result> {

    data class Result(
        val projectId: UUID,
        val name: String,
        val status: ProjectStatus,
        val sourceType: SourceType,
    )
}
