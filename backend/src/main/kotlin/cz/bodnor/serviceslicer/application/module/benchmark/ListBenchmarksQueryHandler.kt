package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.query.ListBenchmarksQuery
import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class ListBenchmarksQueryHandler(
    private val benchmarkReadService: BenchmarkReadService,
) : QueryHandler<Page<Benchmark>, ListBenchmarksQuery> {
    override val query = ListBenchmarksQuery::class

    override fun handle(query: ListBenchmarksQuery): Page<Benchmark> = benchmarkReadService.findAll(query.toPageable())
}
