package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.analysis.command.RunJobCommand
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/projects/{projectId}/jobs")
class JobController(
    private val commandBus: CommandBus,
) {

    @PostMapping
    fun runJob(
        @PathVariable projectId: UUID,
        @RequestBody request: RunJobRequest,
    ) {
        commandBus(request.toCommand(projectId))
    }
}

data class RunJobRequest(
    val jobType: JobType,
) {
    fun toCommand(projectId: UUID) = RunJobCommand(
        projectId = projectId,
        jobType = jobType,
    )
}
