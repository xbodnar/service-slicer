package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.query.ListBenchmarksQuery
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class ListBenchmarksQueryHandler(
    private val benchmarkReadService: BenchmarkReadService,
) : QueryHandler<ListBenchmarksQuery.Result, ListBenchmarksQuery> {
    override val query = ListBenchmarksQuery::class

    override fun handle(query: ListBenchmarksQuery): ListBenchmarksQuery.Result {
        val benchmarks = benchmarkReadService.findAll()

        return ListBenchmarksQuery.Result(
            benchmarks = benchmarks.map {
                ListBenchmarksQuery.BenchmarkSummary(
                    benchmarkId = it.id,
                    name = it.name,
                    description = it.description,
                    createdAt = it.createdAt,
                )
            },
        )
    }
}
