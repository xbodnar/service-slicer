package cz.bodnor.serviceslicer.domain.sut

import cz.bodnor.serviceslicer.domain.file.File
import java.util.UUID

data class DatabaseSeedConfig(
    // Reference to the SQL seed file
    val sqlSeedFileId: UUID,
    // Database container name in docker-compose
    val dbContainerName: String,
    // Database port inside container
    val dbPort: Int,
    // Database name
    val dbName: String,
    // Database username
    val dbUsername: String,
) {
    fun withFile(file: File): DatabaseSeedConfigWithFile {
        require(file.id == sqlSeedFileId) { "File ID does not match" }

        return DatabaseSeedConfigWithFile(
            sqlSeedFile = file,
            dbContainerName = dbContainerName,
            dbPort = dbPort,
            dbName = dbName,
            dbUsername = dbUsername,
        )
    }
}

data class DatabaseSeedConfigWithFile(
    val sqlSeedFile: File,
    val dbContainerName: String,
    val dbPort: Int,
    val dbName: String,
    val dbUsername: String,
)
