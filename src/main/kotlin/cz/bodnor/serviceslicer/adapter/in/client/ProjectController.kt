package cz.bodnor.serviceslicer.adapter.`in`.client

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectFromZipCommand
import cz.bodnor.serviceslicer.application.module.project.query.GetProjectQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
        @RequestParam file: MultipartFile,
        @RequestParam projectName: String,
    ): CreateProjectFromZipCommand.Result = commandBus(
        CreateProjectFromZipCommand(
            projectName = projectName,
            file = file,
        ),
    )

    @GetMapping("/{projectId}")
    fun getProject(@PathVariable projectId: UUID): GetProjectQuery.Result =
        queryBus(GetProjectQuery(projectId = projectId))
}
