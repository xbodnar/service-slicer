package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.GetBenchmarkRunQuery
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCase
import cz.bodnor.serviceslicer.domain.testcase.TargetTestCase
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetBenchmarkRunQueryHandler(
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val fileReadService: FileReadService,
) : QueryHandler<GetBenchmarkRunQuery.Result, GetBenchmarkRunQuery> {
    override val query = GetBenchmarkRunQuery::class

    override fun handle(query: GetBenchmarkRunQuery): GetBenchmarkRunQuery.Result {
        val benchmarkRun = benchmarkRunReadService.getById(query.benchmarkRunId)

        return GetBenchmarkRunQuery.Result(
            id = benchmarkRun.id,
            benchmarkId = benchmarkRun.benchmark.id,
            state = benchmarkRun.state,
            baselineTestCase = benchmarkRun.baselineTestCase.toDto(),
            targetTestCases = benchmarkRun.targetTestCases.map { it.toDto() },
            experimentResults = benchmarkRun.experimentResults,
            createdAt = benchmarkRun.createdAt,
            updatedAt = benchmarkRun.updatedAt,
        )
    }

    private fun BaselineTestCase.toDto() = GetBenchmarkRunQuery.BaselineTestCaseDto(
        id = this.id,
        load = this.load,
        baselineSutId = this.baselineSut.id,
        startTimestamp = this.startTimestamp,
        endTimestamp = this.endTimestamp,
        status = this.status,
        k6Output = this.k6Output,
        jsonSummary = this.jsonSummary,
        operationMetrics = this.operationMetrics,
        relativeDomainMetrics = this.relativeDomainMetrics,
    )

    private fun TargetTestCase.toDto() = GetBenchmarkRunQuery.TargetTestCaseDto(
        id = this.id,
        load = this.load,
        loadFrequency = this.loadFrequency,
        status = this.status,
        startTimestamp = this.startTimestamp,
        endTimestamp = this.endTimestamp,
        operationMetrics = this.operationMetrics,
        relativeDomainMetric = this.relativeDomainMetric,
        k6Output = this.k6Output,
        jsonSummary = this.jsonSummary,
    )
}
