package cz.bodnor.serviceslicer.application.module.benchmark.event

import java.util.UUID

data class BenchmarkRunCreatedEvent(
    val benchmarkRunId: UUID,
)
