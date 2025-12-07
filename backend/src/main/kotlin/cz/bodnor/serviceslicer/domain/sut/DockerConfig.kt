package cz.bodnor.serviceslicer.domain.sut

import cz.bodnor.serviceslicer.domain.file.File
import java.util.UUID

data class DockerConfig(
    // Reference to the docker-compose file
    val composeFileId: UUID,
    // Health check endpoint path (e.g., "/actuator/health")
    val healthCheckPath: String,
    // Port on which the application is exposed (based on docker-compose)
    val appPort: Int,
    // Startup timeout in seconds
    val startupTimeoutSeconds: Long,
) {
    fun withFile(file: File): DockerConfigWithFile {
        require(file.id == composeFileId) { "File ID does not match" }

        return DockerConfigWithFile(
            composeFile = file,
            healthCheckPath = healthCheckPath,
            appPort = appPort,
            startupTimeoutSeconds = startupTimeoutSeconds,
        )
    }
}

data class DockerConfigWithFile(
    val composeFile: File,
    val healthCheckPath: String,
    val appPort: Int,
    val startupTimeoutSeconds: Long,
)
