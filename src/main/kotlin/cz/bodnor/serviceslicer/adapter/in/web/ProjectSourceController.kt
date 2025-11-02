package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateGitProjectSourceRequest
import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateZipProjectSourceRequest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects/sources")
class ProjectSourceController(
    private val commandBus: CommandBus,
) {

    @PostMapping("/zip")
    fun createZipProjectSource(@RequestBody body: CreateZipProjectSourceRequest) = commandBus(body.toCommand())

    @PostMapping("/git")
    fun createGitProjectSource(@RequestBody body: CreateGitProjectSourceRequest) = commandBus(body.toCommand())
}
