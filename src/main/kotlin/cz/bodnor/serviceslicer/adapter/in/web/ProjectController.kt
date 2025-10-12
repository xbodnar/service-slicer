package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectFromGitHubCommand
import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectFromZipCommand
import cz.bodnor.serviceslicer.application.module.project.query.GetProjectQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/projects")
class ProjectController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {

    @PostMapping("/upload")
    fun createFromZip(
        @RequestPart file: MultipartFile,
        @RequestParam projectName: String,
        @RequestParam javaProjectRoot: String?,
    ): CreateProjectFromZipCommand.Result = commandBus(
        CreateProjectFromZipCommand(
            projectName = projectName,
            javaProjectRoot = javaProjectRoot,
            file = file,
        ),
    )

    @PostMapping
    fun createFromGithub(@RequestBody request: CreateProjectFromGitHubRequest): CreateProjectFromGitHubCommand.Result =
        commandBus(request.toCommand())

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): GetProjectQuery.Result =
        queryBus(GetProjectQuery(projectId = projectId))

    @PostMapping("/{projectId}/graph")
    fun rebuildGraph(@PathVariable projectId: UUID) {
        commandBus(BuildDependencyGraphCommand(projectId = projectId))
    }
}

data class CreateProjectFromGitHubRequest(
    val projectName: String,
    val gitHubUrl: String,
    val javaProjectRoot: String?,
) {
    fun toCommand(): CreateProjectFromGitHubCommand = CreateProjectFromGitHubCommand(
        projectName = projectName,
        gitHubUrl = gitHubUrl,
        javaProjectRoot = javaProjectRoot,
    )
}
