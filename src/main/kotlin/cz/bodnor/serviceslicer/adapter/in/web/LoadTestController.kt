package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.loadtest.command.CreateLoadTestConfigurationCommand
import cz.bodnor.serviceslicer.domain.loadtest.LoadTestConfig
import cz.bodnor.serviceslicer.domain.loadtest.LoadTestConfigReadService
import cz.bodnor.serviceslicer.domain.loadtest.LoadTestConfigWriteService
import cz.bodnor.serviceslicer.domain.loadtest.OperationalProfile
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/loadtest")
class LoadTestController(
    private val readService: LoadTestConfigReadService,
    private val writeService: LoadTestConfigWriteService,
    private val commandBus: CommandBus,
) {

    @PostMapping
    fun createLoadTestConfiguration(@RequestBody request: CreateLoadTestConfigurationRequest) =
        commandBus(request.toCommand())

    @GetMapping("/{id}")
    fun getLoadTestConfiguration(@PathVariable id: UUID): LoadTestConfig = readService.getById(id)
}

data class CreateLoadTestConfigurationRequest(
    val openApiFileId: UUID,
    val name: String,
    val behaviorModels: List<CreateLoadTestConfigurationCommand.UserBehaviorModel> = emptyList(),
    val operationalProfile: OperationalProfile? = null,
) {

    fun toCommand() = CreateLoadTestConfigurationCommand(
        openApiFileId = openApiFileId,
        name = name,
        behaviorModels = behaviorModels,
        operationalProfile = operationalProfile,
    )
}
