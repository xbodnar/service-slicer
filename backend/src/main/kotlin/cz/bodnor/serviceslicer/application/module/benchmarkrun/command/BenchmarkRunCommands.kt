package cz.bodnor.serviceslicer.application.module.benchmarkrun.command

import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.domain.benchmarkrun.OperationRunMetrics
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

data class RunSutBenchmarkCommand(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
    val targetVus: Int,
) : Command<RunSutBenchmarkCommand.LoadTestResult> {

    data class LoadTestResult(
        val startTimestamp: Instant,
        val endTimestamp: Instant,
        val operationMeasurements: List<OperationRunMetrics>,
        val k6Output: String,
    )
}

data class ValidateSutBenchmarkConfigCommand(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
) : Command<ValidationResult>

data class RunSutValidationCommand(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
) : Command<ValidationResult>

data class CreateBenchmarkRunCommand(
    val benchmarkId: UUID,
) : Command<CreateBenchmarkRunCommand.Result> {
    @Schema(name = "RunBenchmarkResult", description = "Result of running a benchmark")
    data class Result(
        @field:Schema(description = "ID of the created benchmark run")
        val benchmarkRunId: UUID,
    )
}
