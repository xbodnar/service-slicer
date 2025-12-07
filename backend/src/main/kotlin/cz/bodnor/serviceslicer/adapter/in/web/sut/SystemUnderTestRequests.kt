package cz.bodnor.serviceslicer.adapter.`in`.web.sut

import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request to create a system under test")
data class CreateSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: DockerConfig,
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<DatabaseSeedConfig>,
)

@Schema(description = "Request to update a system under test")
data class UpdateSystemUnderTestRequest(
    @field:Schema(description = "Name of the system under test", example = "Monolithic Architecture")
    val name: String,
    @field:Schema(description = "Description of the system under test", example = "Original monolithic implementation")
    val description: String? = null,
    @field:Schema(description = "Docker configuration")
    val dockerConfig: DockerConfig,
    @field:Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<DatabaseSeedConfig>,
)
