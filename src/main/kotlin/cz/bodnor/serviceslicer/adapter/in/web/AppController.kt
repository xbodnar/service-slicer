package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.compose.command.RunAndValidateComposeCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Path

@RestController
@RequestMapping("/apps")
class AppController(
    private val commandBus: CommandBus,
) {

    /**
     * Runs a docker-compose file and validates that the app is healthy. The app is expected to be reachable at port 9090
     */
    @PostMapping
    fun runAndValidate(@RequestBody request: RunAndValidateAppRequest): RunAndValidateComposeCommand.Result =
        commandBus(
            RunAndValidateComposeCommand(
                composeFilePath = request.composeFilePath,
                healthCheckUrl = request.healthCheckUrl,
                startupTimeoutSeconds = request.startupTimeoutSeconds,
            ),
        )
}

data class RunAndValidateAppRequest(
    val composeFilePath: Path,
    val healthCheckUrl: String,
    val startupTimeoutSeconds: Int = 30,
)
