package cz.bodnor.serviceslicer.application.module.compose

import cz.bodnor.serviceslicer.application.module.compose.command.RunAndValidateComposeCommand
import cz.bodnor.serviceslicer.application.module.compose.service.ComposeFileFinderService
import cz.bodnor.serviceslicer.application.module.compose.service.DockerComposeService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component

@Component
class RunAndValidateComposeCommandHandler(
    private val composeFileFinderService: ComposeFileFinderService,
    private val dockerComposeService: DockerComposeService,
) : CommandHandler<RunAndValidateComposeCommand.Result, RunAndValidateComposeCommand> {

    override val command = RunAndValidateComposeCommand::class

    override fun handle(command: RunAndValidateComposeCommand): RunAndValidateComposeCommand.Result {
        val isHealthy = dockerComposeService.runAndValidate(
            composeFilePath = command.composeFilePath,
            healthCheckUrl = command.healthCheckUrl,
            startupTimeoutSeconds = command.startupTimeoutSeconds,
        )

        return RunAndValidateComposeCommand.Result(
            isHealthy = isHealthy,
        )
    }
}
