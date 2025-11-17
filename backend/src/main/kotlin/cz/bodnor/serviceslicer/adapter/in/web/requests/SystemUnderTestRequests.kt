package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to add a system under test to an experiment")
data class AddSystemUnderTestRequest(
    @Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @Schema(description = "ID of the Docker Compose file")
    val composeFileId: UUID,
    @Schema(description = "ID of the JAR file to test")
    val jarFileId: UUID,
    @Schema(description = "ID of the SQL seed file (optional)")
    val sqlSeedFileId: UUID? = null,
    @Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @Schema(description = "Health check endpoint path", example = "/actuator/health")
    val healthCheckPath: String = "/actuator/health",
    @Schema(description = "Application port", example = "9090")
    val appPort: Int = 9090,
    @Schema(description = "Startup timeout in seconds", example = "180")
    val startupTimeoutSeconds: Long = 180,
    @Schema(description = "Database container name in docker-compose (required if sqlSeedFileId is provided)")
    val dbContainerName: String? = null,
    @Schema(description = "Database port inside container (required if sqlSeedFileId is provided)")
    val dbPort: Int? = null,
    @Schema(description = "Database name (required if sqlSeedFileId is provided)")
    val dbName: String? = null,
    @Schema(description = "Database username (required if sqlSeedFileId is provided)")
    val dbUsername: String? = null,
) {

    fun toCommand(experimentId: UUID) = AddSystemUnderTestCommand(
        experimentId = experimentId,
        name = name,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
        jarFileId = jarFileId,
        composeFileId = composeFileId,
        sqlSeedFileId = sqlSeedFileId,
        dbContainerName = dbContainerName,
        dbPort = dbPort,
        dbName = dbName,
        dbUsername = dbUsername,
    )
}

@Schema(description = "Request to update a system under test")
data class UpdateSystemUnderTestRequest(
    @Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @Schema(description = "ID of the Docker Compose file")
    val composeFileId: UUID,
    @Schema(description = "ID of the JAR file to test")
    val jarFileId: UUID,
    @Schema(description = "ID of the SQL seed file (optional)")
    val sqlSeedFileId: UUID? = null,
    @Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @Schema(description = "Health check endpoint path", example = "/actuator/health")
    val healthCheckPath: String = "/actuator/health",
    @Schema(description = "Application port", example = "9090")
    val appPort: Int = 9090,
    @Schema(description = "Startup timeout in seconds", example = "180")
    val startupTimeoutSeconds: Long = 180,
    @Schema(description = "Database container name in docker-compose (required if sqlSeedFileId is provided)")
    val dbContainerName: String? = null,
    @Schema(description = "Database port inside container (required if sqlSeedFileId is provided)")
    val dbPort: Int? = null,
    @Schema(description = "Database name (required if sqlSeedFileId is provided)")
    val dbName: String? = null,
    @Schema(description = "Database username (required if sqlSeedFileId is provided)")
    val dbUsername: String? = null,
) {
    fun toCommand(
        experimentId: UUID,
        sutId: UUID,
    ) = UpdateSystemUnderTestCommand(
        experimentId = experimentId,
        sutId = sutId,
        name = name,
        composeFileId = composeFileId,
        jarFileId = jarFileId,
        sqlSeedFileId = sqlSeedFileId,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
        dbContainerName = dbContainerName,
        dbPort = dbPort,
        dbName = dbName,
        dbUsername = dbUsername,
    )
}
