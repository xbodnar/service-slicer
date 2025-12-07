package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.query.GetBenchmarkRunQuery
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetBenchmarkRunQueryHandler(
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val fileReadService: FileReadService,
) : QueryHandler<BenchmarkRun, GetBenchmarkRunQuery> {
    override val query = GetBenchmarkRunQuery::class

    override fun handle(query: GetBenchmarkRunQuery): BenchmarkRun =
        benchmarkRunReadService.getById(query.benchmarkRunId)
}
