package cz.bodnor.serviceslicer.application.module.benchmarkrun.command

import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID
import kotlin.time.Duration

data class ValidateSutOperationalSettingCommand(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
) : Command<ValidationResult>

data class RunSutValidationCommand(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
) : Command<Unit>

data class CreateBenchmarkRunCommand(
    val benchmarkId: UUID,
    val testDuration: Duration?,
) : Command<CreateBenchmarkRunCommand.Result> {
    @Schema(name = "RunBenchmarkResult", description = "Result of running a benchmark")
    data class Result(
        @field:Schema(description = "ID of the created benchmark run")
        val benchmarkRunId: UUID,
    )
}

data class ExecuteTestCaseCommand(
    val benchmarkRunId: UUID,
) : Command<ExecuteTestCaseCommand.Result> {
    data class Result(
        val hasMoreTests: Boolean,
    )
}
