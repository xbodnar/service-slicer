package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GetBenchmark
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetBenchmarkQueryHandler(
    private val getBenchmark: GetBenchmark,
) : QueryHandler<GetBenchmarkQuery.Result, GetBenchmarkQuery> {
    override val query = GetBenchmarkQuery::class

    override fun handle(query: GetBenchmarkQuery): GetBenchmarkQuery.Result = getBenchmark(query.benchmarkId)
}
