package cz.bodnor.serviceslicer.application.module.benchmark.port.out

import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import java.util.UUID

interface GetBenchmark {
    operator fun invoke(benchmarkId: UUID): GetBenchmarkQuery.Result
}
