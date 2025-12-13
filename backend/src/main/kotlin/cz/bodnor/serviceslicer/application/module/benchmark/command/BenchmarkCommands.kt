package cz.bodnor.serviceslicer.application.module.benchmark.command

import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateBenchmarkCommand(
    val name: String,
    val description: String? = null,
    val operationalSettingId: UUID,
    val systemsUnderTest: List<UUID>,
    val baselineSutId: UUID,
) : Command<Benchmark>

data class UpdateBenchmarkCommand(
    val benchmarkId: UUID,
    val name: String,
    val description: String? = null,
) : Command<Benchmark>

data class GenerateBehaviorModelsCommand(
    val benchmarkId: UUID,
) : Command<GenerateBehaviorModelsCommand.Result> {

    data class Result(
        val loadTestConfigId: UUID,
    )
}
