package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.GetBenchmarkRunQuery
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
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
            baselineTestCase = benchmarkRun.baselineTestCase,
            architectureTestSuites = benchmarkRun.architectureTestSuites,
            createdAt = benchmarkRun.createdAt,
            updatedAt = benchmarkRun.updatedAt,
        )
    }
}
