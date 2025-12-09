package cz.bodnor.serviceslicer.adapter.`in`.web.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.command.RestartDecompositionJobCommand
import cz.bodnor.serviceslicer.application.module.decomposition.query.GetDecompositionJobSummaryQuery
import cz.bodnor.serviceslicer.application.module.decomposition.query.ListDecompositionJobsQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/decomposition-jobs")
class DecompositionJobController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    private val mapper: DecompositionJobMapper,
) {

    @GetMapping
    fun listDecompositionJobs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ListDecompositionJobsResponse = mapper.toDto(queryBus(ListDecompositionJobsQuery(page = page, size = size)))

    @GetMapping("/{decompositionJobId}")
    fun getDecompositionJobSummary(@PathVariable decompositionJobId: UUID): DecompositionJobSummaryDto =
        mapper.toDto(queryBus(GetDecompositionJobSummaryQuery(decompositionJobId = decompositionJobId)))

    @PostMapping
    fun createDecompositionJob(@RequestBody body: CreateDecompositionJobRequest): DecompositionJobDto =
        mapper.toDto(commandBus(mapper.toCommand(body)))

    @PostMapping("/{decompositionJobId}/restart")
    fun restartDecompositionJob(@PathVariable decompositionJobId: UUID): DecompositionJobDto =
        mapper.toDto(commandBus(RestartDecompositionJobCommand(decompositionJobId = decompositionJobId)))
}
