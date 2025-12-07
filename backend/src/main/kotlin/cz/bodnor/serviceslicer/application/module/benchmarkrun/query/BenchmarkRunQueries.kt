package cz.bodnor.serviceslicer.application.module.benchmarkrun.query

import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

data class ListBenchmarkRunsQuery(
    val benchmarkId: UUID,
    val page: Int = 0,
    val size: Int = 10,
) : Query<Page<BenchmarkRun>> {
    fun toPageable() = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTimestamp"))
}

data class GetBenchmarkRunQuery(val benchmarkRunId: UUID) : Query<BenchmarkRun>
