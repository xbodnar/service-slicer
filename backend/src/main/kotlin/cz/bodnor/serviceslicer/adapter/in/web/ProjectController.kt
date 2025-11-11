package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateProjectRequest
import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.project.query.GetProjectQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects")
class ProjectController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {
    @PostMapping
    fun createProject(@RequestBody body: CreateProjectRequest) = commandBus(body.toCommand())

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): GetProjectQuery.Result =
        queryBus(GetProjectQuery(projectId = projectId))

    @PostMapping("/{projectId}/graph")
    fun rebuildGraph(@PathVariable projectId: UUID) {
        commandBus(BuildDependencyGraphCommand(projectId = projectId))
    }
}
