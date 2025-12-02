package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.benchmark.query.FileDto
import cz.bodnor.serviceslicer.application.module.sut.query.DatabaseSeedConfigDto
import cz.bodnor.serviceslicer.application.module.sut.query.DockerConfigDto
import cz.bodnor.serviceslicer.application.module.sut.query.GetSystemUnderTestQuery
import cz.bodnor.serviceslicer.application.module.sut.query.SystemUnderTestDto
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component

@Component
class GetSystemUnderTestQueryHandler(
    private val sutReadService: SystemUnderTestReadService,
    private val fileReadService: FileReadService,
) : QueryHandler<SystemUnderTestDto, GetSystemUnderTestQuery> {
    override val query = GetSystemUnderTestQuery::class

    override fun handle(query: GetSystemUnderTestQuery): SystemUnderTestDto {
        val sut = sutReadService.getById(query.sutId)
        val dockerComposeFile = fileReadService.getById(sut.dockerConfig.composeFileId)

        return SystemUnderTestDto(
            id = sut.id,
            name = sut.name,
            description = sut.description,
            dockerConfig = DockerConfigDto(
                composeFile = FileDto(
                    fileId = dockerComposeFile.id,
                    filename = dockerComposeFile.filename,
                    fileSize = dockerComposeFile.expectedSize,
                ),
                healthCheckPath = sut.dockerConfig.healthCheckPath,
                appPort = sut.dockerConfig.appPort,
                startupTimeoutSeconds = sut.dockerConfig.startupTimeoutSeconds,
            ),
            databaseSeedConfigs = sut.databaseSeedConfigs.map { config ->
                val sqlSeedFile = fileReadService.getById(config.sqlSeedFileId)
                DatabaseSeedConfigDto(
                    sqlSeedFile = FileDto(
                        fileId = config.sqlSeedFileId,
                        filename = sqlSeedFile.filename,
                        fileSize = sqlSeedFile.expectedSize,
                    ),
                    dbContainerName = config.dbContainerName,
                    dbPort = config.dbPort,
                    dbName = config.dbName,
                    dbUsername = config.dbUsername,
                )
            },
        )
    }
}
