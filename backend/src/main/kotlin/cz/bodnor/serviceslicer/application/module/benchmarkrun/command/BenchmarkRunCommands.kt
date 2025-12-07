package cz.bodnor.serviceslicer.application.module.benchmarkrun.command

import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

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
    val testDuration: String?,
) : Command<BenchmarkRun>

data class ExecuteTestCaseCommand(
    val benchmarkRunId: UUID,
) : Command<ExecuteTestCaseCommand.Result> {
    data class Result(
        val hasMoreTests: Boolean,
    )
}
