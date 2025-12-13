package cz.bodnor.serviceslicer.application.module.benchmark.query

import cz.bodnor.serviceslicer.adapter.`in`.web.benchmark.BenchmarkSystemUnderTestDto
import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

data class ListBenchmarksQuery(
    val page: Int = 0,
    val size: Int = 10,
) : Query<Page<Benchmark>> {
    fun toPageable() = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTimestamp"))
}

data class GetBenchmarkQuery(val benchmarkId: UUID) : Query<GetBenchmarkQuery.Result> {
    data class Result(
        val benchmark: Benchmark,
        val systemsUnderTest: List<BenchmarkSystemUnderTestDto>,
    )
}
