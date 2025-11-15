package cz.bodnor.serviceslicer.application.module.loadtestexperiment.command

import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand
import cz.bodnor.serviceslicer.application.module.loadtestconfig.command.CreateLoadTestConfigCommand.CreateUserBehaviorModelDto
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateLoadTestExperimentCommand(
    val name: String,
    val description: String? = null,
    val loadTestConfig: CreateLoadTestConfigCommand,
    val systemsUnderTest: List<CreateSystemUnderTest>,
) : Command<CreateLoadTestExperimentCommand.Result> {

    @Schema(description = "System under test configuration")
    data class CreateSystemUnderTest(
        @Schema(description = "Name of the system under test", example = "Monolithic Architecture")
        val name: String,
        @Schema(description = "ID of the Docker Compose file")
        val composeFileId: UUID,
        @Schema(description = "ID of the JAR file to test")
        val jarFileId: UUID,
        @Schema(description = "Description of the system under test")
        val description: String? = null,
        @Schema(description = "Health check endpoint path", example = "/actuator/health")
        val healthCheckPath: String = "/actuator/health",
        @Schema(description = "Application port", example = "9090")
        val appPort: Int = 9090,
        @Schema(description = "Startup timeout in seconds", example = "180")
        val startupTimeoutSeconds: Long = 180,
    )

    @Schema(name = "CreateLoadTestExperimentResult", description = "Result of creating a load test experiment")
    data class Result(
        @Schema(description = "ID of the created experiment")
        val experimentId: UUID,
    )
}
