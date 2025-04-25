package cz.bodnor.serviceslicer.adapter.`in`.client

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
        @RequestPart request: CreateProjectFromZipRequest,
    ): CreateProjectFromZipCommand.Result = commandBus(request.toCommand(file))

    @PostMapping
    fun createFromGithub(@RequestBody request: CreateProjectFromGitHubRequest): CreateProjectFromGitHubCommand.Result =
        commandBus(request.toCommand())

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): GetProjectQuery.Result =
        queryBus(GetProjectQuery(projectId = projectId))
}

data class CreateProjectFromZipRequest(
    val projectName: String,
    val javaProjectRoot: String?,
) {
    fun toCommand(file: MultipartFile): CreateProjectFromZipCommand = CreateProjectFromZipCommand(
        projectName = projectName,
        javaProjectRoot = javaProjectRoot,
        file = file,
    )
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
