package cz.bodnor.serviceslicer.adapter.`in`.web.benchmarkrun

import java.util.UUID

data class CreateBenchmarkRunRequest(
    val benchmarkId: UUID,
    val testDuration: String? = null,
)
