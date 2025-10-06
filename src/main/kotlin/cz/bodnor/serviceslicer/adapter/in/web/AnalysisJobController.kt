package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.service.RestartFailedAnalysisJobService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/analysis")
class AnalysisJobController(
    private val commandBus: CommandBus,
    private val restartFailedAnalysisJobService: RestartFailedAnalysisJobService,
) {

    @PostMapping("/project/{projectId}/run")
    fun run(@PathVariable projectId: UUID) = commandBus(RunAnalysisJobCommand(projectId))

    @PostMapping("/jobs/{analysisJobId}/restart")
    fun restartFailedJob(@PathVariable analysisJobId: UUID): ResponseEntity<Map<String, String>> = try {
        restartFailedAnalysisJobService.restart(analysisJobId)
        ResponseEntity.ok(mapOf("message" to "Job restarted successfully", "jobId" to analysisJobId.toString()))
    } catch (e: IllegalArgumentException) {
        ResponseEntity.badRequest().body(mapOf("error" to e.message.orEmpty()))
    } catch (e: IllegalStateException) {
        ResponseEntity.badRequest().body(mapOf("error" to e.message.orEmpty()))
    }
}
