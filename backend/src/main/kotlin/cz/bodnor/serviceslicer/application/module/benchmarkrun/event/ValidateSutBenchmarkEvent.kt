package cz.bodnor.serviceslicer.application.module.benchmarkrun.event

import java.util.UUID

data class ValidateSutBenchmarkEvent(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
)
