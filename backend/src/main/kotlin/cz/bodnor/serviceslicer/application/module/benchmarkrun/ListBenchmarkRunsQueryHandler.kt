package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.ListBenchmarkRunsQuery
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class ListBenchmarkRunsQueryHandler(
    private val benchmarkRunRepository: BenchmarkRunRepository,
) : QueryHandler<ListBenchmarkRunsQuery.Result, ListBenchmarkRunsQuery> {
    override val query = ListBenchmarkRunsQuery::class

    override fun handle(query: ListBenchmarkRunsQuery): ListBenchmarkRunsQuery.Result {
        val benchmarkRuns = benchmarkRunRepository.findAllByBenchmarkId(query.benchmarkId)

        return ListBenchmarkRunsQuery.Result(
            benchmarkRuns = benchmarkRuns.map { run ->
                ListBenchmarkRunsQuery.BenchmarkRunSummary(
                    benchmarkRunId = run.id,
                    benchmarkId = run.benchmarkId,
                    state = run.state,
                    sutCount = run.architectureTestSuites.size,
                    createdAt = run.createdAt,
                    updatedAt = run.updatedAt,
                )
            },
        )
    }
}
