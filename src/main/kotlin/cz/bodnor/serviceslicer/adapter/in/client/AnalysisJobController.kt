package cz.bodnor.serviceslicer.adapter.`in`.client

import cz.bodnor.serviceslicer.application.module.analysis.command.CreateAnalysisJobCommand
import cz.bodnor.serviceslicer.application.module.analysis.command.RunAnalysisJobCommand
import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/analysis/jobs")
class AnalysisJobController(
    private val commandBus: CommandBus,
) {

    @PostMapping
    fun create(@RequestBody request: CreateAnalysisJobRequest) = commandBus(request.toCommand())

    @PostMapping("/{analysisJobId}/run")
    fun run(@PathVariable analysisJobId: UUID) = commandBus(RunAnalysisJobCommand(analysisJobId))
}

data class CreateAnalysisJobRequest(
    val projectId: UUID,
    val type: AnalysisType,
) {
    fun toCommand() = CreateAnalysisJobCommand(projectId, type)
}
