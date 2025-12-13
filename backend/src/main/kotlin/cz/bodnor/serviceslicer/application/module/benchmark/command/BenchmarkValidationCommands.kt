package cz.bodnor.serviceslicer.application.module.benchmark.command

import cz.bodnor.serviceslicer.domain.benchmarkvalidation.BenchmarkSutValidationRun
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateBenchmarkValidationRunCommand(
    val benchmarkId: UUID,
    val systemUnderTestId: UUID,
) : Command<BenchmarkSutValidationRun>

data class ExecuteBenchmarkValidationCommand(
    val benchmarkValidationRunId: UUID,
) : Command<Unit>
