package cz.bodnor.serviceslicer.application.module.sut.query

import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class GetSystemUnderTestQuery(val sutId: UUID) : Query<SystemUnderTestDto>

class ListSystemsUnderTestQuery() : Query<ListSystemsUnderTestQuery.Result> {

    data class Result(
        val systemsUnderTest: List<SystemUnderTest>,
    )
}

@Schema(description = "System under test")
data class SystemUnderTestDto(
    @Schema(description = "ID of the system under test")
    val id: UUID,
    @Schema(description = "Name of the system under test")
    val name: String,
    @Schema(description = "Description of the system under test")
    val description: String?,
    @Schema(description = "Docker configuration")
    val dockerConfig: DockerConfigDto,
    @Schema(description = "Database seed configurations (one per database)")
    val databaseSeedConfigs: List<DatabaseSeedConfigDto>,
)

data class DockerConfigDto(
    @field:Schema(description = "ID of the Docker Compose file")
    val composeFile: File,
    @field:Schema(description = "Health check endpoint path")
    val healthCheckPath: String,
    @field:Schema(description = "Application port")
    val appPort: Int,
    @field:Schema(description = "Startup timeout in seconds")
    val startupTimeoutSeconds: Long,
) {
    fun toDomain() = DockerConfig(
        composeFileId = composeFile.id,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}

data class DatabaseSeedConfigDto(
    @field:Schema(description = "ID of the SQL seed file")
    val sqlSeedFile: File,
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
        sqlSeedFileId = sqlSeedFile.id,
        dbContainerName = dbContainerName,
        dbPort = dbPort,
        dbName = dbName,
        dbUsername = dbUsername,
    )
}
