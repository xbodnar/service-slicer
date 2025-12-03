package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkCommand
import cz.bodnor.serviceslicer.application.module.benchmark.command.UpdateBenchmarkCommand
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a benchmark")
data class CreateBenchmarkRequest(
    @field:Schema(description = "Name of the benchmark", example = "Baseline Performance Test")
    val name: String,
    @field:Schema(
        description = "Description of the benchmark",
        example = "Testing baseline performance with monolithic architecture",
    )
    val description: String? = null,
    @field:Schema(description = "Load test configuration")
    val operationalSetting: OperationalSetting,
    @field:Schema(description = "ID of the baseline system under test")
    val baselineSutId: UUID,
    @field:Schema(description = "ID of the target system under test")
    val targetSutId: UUID,
) {

    fun toCommand() = CreateBenchmarkCommand(
        name = name,
        description = description,
        operationalSetting = operationalSetting,
        baselineSutId = baselineSutId,
        targetSutId = targetSutId,
    )
}

@Schema(description = "Request to update a benchmark")
data class UpdateBenchmarkRequest(
    @field:Schema(description = "Name of the benchmark", example = "Baseline Performance Test")
    val name: String,
    @field:Schema(
        description = "Description of the benchmark",
        example = "Testing baseline performance with monolithic architecture",
    )
    val description: String? = null,
    @field:Schema(description = "Load test configuration")
    val operationalSetting: OperationalSetting,
) {
    fun toCommand(benchmarkId: UUID) = UpdateBenchmarkCommand(
        benchmarkId = benchmarkId,
        name = name,
        description = description,
        operationalSetting = operationalSetting,
    )
}
