package cz.bodnor.serviceslicer.adapter.`in`.web.sut

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileDto
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.util.UUID

@Schema(description = "List of systems under test")
data class ListSystemsUnderTestResponse(
    val items: List<SystemUnderTestDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

@Schema(description = "System under test")
data class SystemUnderTestDto(
    val id: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
    val dockerConfig: DockerConfig,
    val databaseSeedConfigs: List<DatabaseSeedConfig>,
)

@Schema(description = "System under test details")
data class SystemUnderTestDetailDto(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
    val dockerConfig: DockerConfigDto,
    val databaseSeedConfigs: List<DatabaseSeedConfigDto>,
)

@Schema(description = "Docker configuration")
data class DockerConfigDto(
    val composeFile: FileDto,
    val healthCheckPath: String,
    val appPort: Int,
    val startupTimeoutSeconds: Long,
)

@Schema(description = "Database seed configuration")
data class DatabaseSeedConfigDto(
    val sqlSeedFile: FileDto,
    val dbContainerName: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
)
