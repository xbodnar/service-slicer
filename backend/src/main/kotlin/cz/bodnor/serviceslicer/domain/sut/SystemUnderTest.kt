package cz.bodnor.serviceslicer.domain.sut

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import cz.bodnor.serviceslicer.domain.file.File
import jakarta.persistence.Entity
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Represents a system configuration under test.
 * Each SUT has a docker-compose file and a JAR file that define the system's deployment.
 */
@Entity
class SystemUnderTest(
    var name: String,
    // Description of this system configuration (e.g., "Baseline monolith", "3-service decomposition")
    var description: String? = null,
    // Docker configuration
    @JdbcTypeCode(SqlTypes.JSON)
    var dockerConfig: DockerConfig,
    // Database seeding configurations (one per database for microservices)
    @JdbcTypeCode(SqlTypes.JSON)
    var databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) : UpdatableEntity() {

    fun getFileIds(): Set<UUID> = setOf(dockerConfig.composeFileId) + databaseSeedConfigs.map { it.sqlSeedFileId }

    fun withFiles(files: List<File>): SystemUnderTestWithFiles {
        val filesMap = files.associateBy { it.id }

        return SystemUnderTestWithFiles(
            id = this.id,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            name = this.name,
            description = this.description,
            dockerConfig = dockerConfig.withFile(filesMap[dockerConfig.composeFileId]!!),
            databaseSeedConfigs = databaseSeedConfigs.map { it.withFile(filesMap[it.sqlSeedFileId]!!) },
        )
    }
}

data class SystemUnderTestWithFiles(
    val id: UUID,
    val createdAt: Instant,
    val updatedAt: Instant,
    val name: String,
    val description: String?,
    val dockerConfig: DockerConfigWithFile,
    val databaseSeedConfigs: List<DatabaseSeedConfigWithFile>,
)

@Repository
interface SystemUnderTestRepository : JpaRepository<SystemUnderTest, UUID>
