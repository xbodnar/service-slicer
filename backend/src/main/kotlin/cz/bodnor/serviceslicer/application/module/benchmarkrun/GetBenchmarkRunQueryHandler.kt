package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.GetBenchmarkRunQuery
import cz.bodnor.serviceslicer.domain.benchmarkrun.ArchitectureTestSuite
import cz.bodnor.serviceslicer.domain.benchmarkrun.BaselineTestCase
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.TargetTestCase
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.stereotype.Component

@Component
class GetBenchmarkRunQueryHandler(
    private val benchmarkRunReadService: BenchmarkRunReadService,
) : QueryHandler<GetBenchmarkRunQuery.Result, GetBenchmarkRunQuery> {
    override val query = GetBenchmarkRunQuery::class

    override fun handle(query: GetBenchmarkRunQuery): GetBenchmarkRunQuery.Result {
        val benchmarkRun = benchmarkRunReadService.getById(query.benchmarkRunId)
        verify(benchmarkRun.benchmarkId == query.benchmarkId) {
            "Benchmark run ${query.benchmarkRunId} does not belong to benchmark ${query.benchmarkId}"
        }

        return GetBenchmarkRunQuery.Result(
            benchmarkRunId = benchmarkRun.id,
            benchmarkId = benchmarkRun.benchmarkId,
            state = benchmarkRun.state,
            baselineTestCase = benchmarkRun.baselineTestCase?.toDto(),
            architectureTestSuites = benchmarkRun.architectureTestSuites.map { it.toDto() },
            createdAt = benchmarkRun.createdAt,
            updatedAt = benchmarkRun.updatedAt,
        )
    }

    private fun BaselineTestCase.toDto() = GetBenchmarkRunQuery.BaselineTestCaseDto(
        id = this.id,
        baselineSutId = this.baselineSutId,
        load = this.load,
        status = this.status,
        startTimestamp = this.startTimestamp,
        endTimestamp = this.endTimestamp,
        operationMetrics = this.operationMetrics,
        k6Output = this.k6Output,
    )

    private fun ArchitectureTestSuite.toDto() = GetBenchmarkRunQuery.ArchitectureTestSuiteDto(
        id = this.id,
        targetSutId = this.targetSutId,
        status = this.status,
        targetTestCases = this.targetTestCases.map { it.toDto() },
        scalabilityFootprint = this.scalabilityFootprint,
        totalDomainMetric = this.totalDomainMetric,
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
