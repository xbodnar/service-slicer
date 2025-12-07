package cz.bodnor.serviceslicer.adapter.`in`.web.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.command.GenerateBehaviorModelsCommand
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.application.module.benchmark.query.ListBenchmarksQuery
import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ValidateSutOperationalSettingCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/benchmarks")
class BenchmarkController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    private val mapper: BenchmarkMapper,
) {

    @GetMapping
    fun listBenchmarks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ListBenchmarksResponse = mapper.toDto(queryBus(ListBenchmarksQuery(page = page, size = size)))

    @GetMapping("/{benchmarkId}")
    fun getBenchmark(@PathVariable benchmarkId: UUID): BenchmarkDetailDto =
        mapper.toDto(queryBus(GetBenchmarkQuery(benchmarkId)))

    @PostMapping
    fun createBenchmark(@RequestBody request: CreateBenchmarkRequest): BenchmarkDto =
        mapper.toDto(commandBus(mapper.toCommand(request)))

    @PutMapping("/{benchmarkId}")
    fun updateBenchmark(
        @PathVariable benchmarkId: UUID,
        @RequestBody request: UpdateBenchmarkRequest,
    ): BenchmarkDto = mapper.toDto(commandBus(mapper.toCommand(request, benchmarkId)))

    @PostMapping("/{benchmarkId}/generate-bm")
    fun generateBehaviorModels(@PathVariable benchmarkId: UUID) =
        commandBus(GenerateBehaviorModelsCommand(benchmarkId = benchmarkId))

    @PostMapping("/{benchmarkId}/sut/{systemUnderTestId}/validate")
    fun validateOperationalSetting(
        @PathVariable benchmarkId: UUID,
        @PathVariable systemUnderTestId: UUID,
    ) = commandBus(
        ValidateSutOperationalSettingCommand(
            benchmarkId = benchmarkId,
            systemUnderTestId = systemUnderTestId,
        ),
    )
}
