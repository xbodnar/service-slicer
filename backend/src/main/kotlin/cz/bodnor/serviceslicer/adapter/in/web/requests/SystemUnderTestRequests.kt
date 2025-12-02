package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.sut.command.CreateSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.query.DatabaseSeedConfigDto
import cz.bodnor.serviceslicer.application.module.sut.query.DockerConfigDto
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a system under test")
data class CreateSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: DockerConfig,
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) {
    fun toCommand() = CreateSystemUnderTestCommand(
        name = name,
        description = description,
        dockerConfig = dockerConfig,
        databaseSeedConfigs = databaseSeedConfigs,
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
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<DatabaseSeedConfigDto> = emptyList(),
) {
    fun toCommand(sutId: UUID) = UpdateSystemUnderTestCommand(
        sutId = sutId,
        name = name,
        description = description,
        dockerConfig = dockerConfig.toDomain(),
        databaseSeedConfigs = databaseSeedConfigs.map { it.toDomain() },
    )
}
