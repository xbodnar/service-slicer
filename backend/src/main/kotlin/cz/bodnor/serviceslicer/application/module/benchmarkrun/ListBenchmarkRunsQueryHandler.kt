package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.ListBenchmarkRunsQuery
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component

@Component
class ListBenchmarkRunsQueryHandler(
    private val benchmarkRunRepository: BenchmarkRunRepository,
) : QueryHandler<Page<BenchmarkRun>, ListBenchmarkRunsQuery> {
    override val query = ListBenchmarkRunsQuery::class

    override fun handle(query: ListBenchmarkRunsQuery): Page<BenchmarkRun> =
        benchmarkRunRepository.findAllByBenchmarkId(query.benchmarkId, query.toPageable())
}
