package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.CreateLoadTestExperimentCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/load-tests/experiments")
class LoadTestExperimentsController(
    private val commandBus: CommandBus,
) {

    @PostMapping
    fun createExperiment(@RequestBody request: CreateLoadTestExperimentRequest) = commandBus(request.toCommand())
}

data class CreateLoadTestExperimentRequest(
    val name: String,
    val description: String? = null,
    val loadTestConfig: CreateLoadTestExperimentCommand.CreateLoadTestConfigDto,
    val systemsUnderTest: List<CreateLoadTestExperimentCommand.CreateSystemUnderTestDto>,
) {
    fun toCommand() = CreateLoadTestExperimentCommand(
        name = name,
        description = description,
        loadTestConfig = loadTestConfig,
        systemsUnderTest = systemsUnderTest,
    )
}
