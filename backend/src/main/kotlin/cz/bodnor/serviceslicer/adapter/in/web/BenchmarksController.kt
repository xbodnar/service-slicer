package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateBenchmarkRequest
import cz.bodnor.serviceslicer.adapter.`in`.web.requests.UpdateBenchmarkRequest
import cz.bodnor.serviceslicer.application.module.benchmark.command.GenerateBehaviorModelsCommand
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.application.module.benchmark.query.ListBenchmarksQuery
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.CreateBenchmarkRunCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ValidateSutBenchmarkConfigCommand
import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.GetBenchmarkRunQuery
import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.ListBenchmarkRunsQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/benchmarks")
class BenchmarksController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {

    @GetMapping
    fun listBenchmarks(): ListBenchmarksQuery.Result = queryBus(ListBenchmarksQuery())

    @PostMapping
    fun createBenchmark(@RequestBody request: CreateBenchmarkRequest) = commandBus(request.toCommand())

    @GetMapping("/{benchmarkId}")
    fun getBenchmark(@PathVariable benchmarkId: UUID): GetBenchmarkQuery.Result =
        queryBus(GetBenchmarkQuery(benchmarkId = benchmarkId))

    @PutMapping("/{benchmarkId}")
    fun updateBenchmark(
        @PathVariable benchmarkId: UUID,
        @RequestBody request: UpdateBenchmarkRequest,
    ) = commandBus(request.toCommand(benchmarkId))

    @PostMapping("/{benchmarkId}/run")
    fun runBenchmark(@PathVariable benchmarkId: UUID) = commandBus(CreateBenchmarkRunCommand(benchmarkId = benchmarkId))

    @PostMapping("/{benchmarkId}/generate-bm")
    fun generateBehaviorModels(@PathVariable benchmarkId: UUID) =
        commandBus(GenerateBehaviorModelsCommand(benchmarkId = benchmarkId))

    @PostMapping("/{benchmarkId}/sut/{systemUnderTestId}/validate")
    fun validateBenchmarkConfig(
        @PathVariable benchmarkId: UUID,
        @PathVariable systemUnderTestId: UUID,
    ) = commandBus(
        ValidateSutBenchmarkConfigCommand(
            benchmarkId = benchmarkId,
            systemUnderTestId = systemUnderTestId,
        ),
    )

    @GetMapping("/{benchmarkId}/runs")
    fun listBenchmarkRuns(@PathVariable benchmarkId: UUID): ListBenchmarkRunsQuery.Result =
        queryBus(ListBenchmarkRunsQuery(benchmarkId = benchmarkId))

    @GetMapping("/{benchmarkId}/runs/{runId}")
    fun getBenchmarkRun(
        @PathVariable benchmarkId: UUID,
        @PathVariable runId: UUID,
    ): GetBenchmarkRunQuery.Result = queryBus(GetBenchmarkRunQuery(benchmarkId = benchmarkId, benchmarkRunId = runId))
}
