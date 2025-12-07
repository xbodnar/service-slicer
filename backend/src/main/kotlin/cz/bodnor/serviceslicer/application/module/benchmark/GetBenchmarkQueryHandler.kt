package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetBenchmarkQueryHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val fileReadService: FileReadService,
) : QueryHandler<GetBenchmarkQuery.Result, GetBenchmarkQuery> {
    override val query = GetBenchmarkQuery::class

    override fun handle(query: GetBenchmarkQuery): GetBenchmarkQuery.Result {
        val benchmark = benchmarkReadService.getById(query.benchmarkId)
        val fileIds =
            benchmark.baselineSut.getFileIds() + benchmark.targetSut.getFileIds()
        val files = fileReadService.findAllByIds(fileIds)

        return GetBenchmarkQuery.Result(
            benchmark = benchmark,
            baselineSut = benchmark.baselineSut.withFiles(files),
            targetSut = benchmark.targetSut.withFiles(files),
        )
    }
}
