package cz.bodnor.serviceslicer.adapter.`in`.web.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.GetBenchmarkRunQuery
import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.ListBenchmarkRunsQuery
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
@RequestMapping("/benchmark-runs")
class BenchmarkRunController(
    private val queryBus: QueryBus,
    private val commandBus: CommandBus,
    private val mapper: BenchmarkRunMapper,
) {

    @PostMapping
    fun createBenchmarkRun(@RequestBody request: CreateBenchmarkRunRequest): BenchmarkRunDto =
        mapper.toDto(commandBus(mapper.toCommand(request)))

    @GetMapping
    fun listBenchmarkRuns(
        @RequestParam benchmarkId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ListBenchmarkRunsResponse = mapper.toDto(queryBus(ListBenchmarkRunsQuery(benchmarkId = benchmarkId)))

    @GetMapping("/{benchmarkRunId}")
    fun getBenchmarkRun(@PathVariable benchmarkRunId: UUID): BenchmarkRunDto =
        mapper.toDto(queryBus(GetBenchmarkRunQuery(benchmarkRunId)))
}
