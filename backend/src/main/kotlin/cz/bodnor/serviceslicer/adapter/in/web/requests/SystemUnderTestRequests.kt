package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.loadtestexperiment.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.loadtestexperiment.DockerConfig
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to add a system under test to an experiment")
data class AddSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Whether this is the baseline system under test")
    val isBaseline: Boolean,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: DockerConfigDto,
    @field:Schema(description = "Database seed configuration (optional)")
    val databaseSeedConfig: DatabaseSeedConfigDto? = null,
) {

    fun toCommand(experimentId: UUID) = AddSystemUnderTestCommand(
        experimentId = experimentId,
        name = name,
        description = description,
        isBaseline = isBaseline,
        dockerConfig = dockerConfig.toDomain(),
        databaseSeedConfig = databaseSeedConfig?.toDomain(),
    )
}

@Schema(description = "Request to update a system under test")
data class UpdateSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: DockerConfigDto,
    @field:Schema(description = "Database seed configuration (optional)")
    val databaseSeedConfig: DatabaseSeedConfigDto? = null,
) {
    fun toCommand(
        experimentId: UUID,
        sutId: UUID,
    ) = UpdateSystemUnderTestCommand(
        experimentId = experimentId,
        sutId = sutId,
        name = name,
        description = description,
        dockerConfig = dockerConfig.toDomain(),
        databaseSeedConfig = databaseSeedConfig?.toDomain(),
    )
}

data class DockerConfigDto(
    @field:Schema(description = "ID of the Docker Compose file")
    val composeFileId: UUID,
    @field:Schema(description = "Health check endpoint path")
    val healthCheckPath: String,
    @field:Schema(description = "Application port")
    val appPort: Int,
    @field:Schema(description = "Startup timeout in seconds")
    val startupTimeoutSeconds: Long,
) {
    fun toDomain() = DockerConfig(
        composeFileId = composeFileId,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}

data class DatabaseSeedConfigDto(
    @field:Schema(description = "ID of the SQL seed file")
    val sqlSeedFileId: UUID,
    @field:Schema(description = "Database container name in docker-compose")
    val dbContainerName: String,
    @field:Schema(description = "Database port inside container")
    val dbPort: Int,
    @field:Schema(description = "Database name")
    val dbName: String,
    @field:Schema(description = "Database username")
    val dbUsername: String,
) {
    fun toDomain() = DatabaseSeedConfig(
        sqlSeedFileId = sqlSeedFileId,
        dbContainerName = dbContainerName,
        dbPort = dbPort,
        dbName = dbName,
        dbUsername = dbUsername,
    )
}
