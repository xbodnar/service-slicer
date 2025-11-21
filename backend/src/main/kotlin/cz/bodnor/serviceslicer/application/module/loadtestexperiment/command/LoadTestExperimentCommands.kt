package cz.bodnor.serviceslicer.application.module.loadtestexperiment.command

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.AddSystemUnderTestRequest
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateLoadTestExperimentCommand(
    val name: String,
    val description: String? = null,
    val loadTestConfig: CreateLoadTestConfigCommand,
    val systemsUnderTest: List<AddSystemUnderTestRequest>,
) : Command<CreateLoadTestExperimentCommand.Result> {

    @Schema(name = "CreateLoadTestExperimentResult", description = "Result of creating a load test experiment")
    data class Result(
        @field:Schema(description = "ID of the created experiment")
        val experimentId: UUID,
    )
}
