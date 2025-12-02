package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.application.module.benchmark.query.FileDto
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.application.module.sut.query.DatabaseSeedConfigDto
import cz.bodnor.serviceslicer.application.module.sut.query.DockerConfigDto
import cz.bodnor.serviceslicer.application.module.sut.query.SystemUnderTestDto
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetBenchmarkQueryHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val fileReadService: FileReadService,
) : QueryHandler<GetBenchmarkQuery.Result, GetBenchmarkQuery> {
    override val query = GetBenchmarkQuery::class

    override fun handle(query: GetBenchmarkQuery): GetBenchmarkQuery.Result {
        val benchmark = benchmarkReadService.getById(query.benchmarkId)
        val fileIds =
            benchmark.baselineSut.getFileIds() + benchmark.targetSut.getFileIds() + benchmark.config.openApiFileId
        val files = fileReadService.findAllByIds(fileIds.toSet()).map { it.toDto() }.associateBy { it.fileId }

        return GetBenchmarkQuery.Result(
            id = benchmark.id,
            name = benchmark.name,
            description = benchmark.description,
            config = benchmark.config.toDto(files),
            baselineSut = benchmark.baselineSut.toDto(files),
            baselineSutValidationResult = benchmark.baselineSutValidationResult,
            targetSut = benchmark.targetSut.toDto(files),
            targetSutValidationResult = benchmark.targetSutValidationResult,
            createdAt = benchmark.createdAt,
            updatedAt = benchmark.updatedAt,
        )
    }

    private fun File.toDto() = FileDto(
        fileId = this.id,
        filename = this.filename,
        fileSize = this.expectedSize,
    )

    private fun SystemUnderTest.getFileIds(): List<UUID> = databaseSeedConfigs.map { it.sqlSeedFileId } +
        dockerConfig.composeFileId

    private fun SystemUnderTest.toDto(files: Map<UUID, FileDto>) = SystemUnderTestDto(
        id = this.id,
        name = this.name,
        description = this.description,
        dockerConfig = this.dockerConfig.toDto(files),
        databaseSeedConfigs = this.databaseSeedConfigs.map { it.toDto(files) },
    )

    private fun DatabaseSeedConfig.toDto(files: Map<UUID, FileDto>) = DatabaseSeedConfigDto(
        sqlSeedFile = files[this.sqlSeedFileId]!!,
        dbContainerName = this.dbContainerName,
        dbPort = this.dbPort,
        dbName = this.dbName,
        dbUsername = this.dbUsername,
    )

    private fun DockerConfig.toDto(files: Map<UUID, FileDto>) = DockerConfigDto(
        composeFile = files[this.composeFileId]!!,
        healthCheckPath = this.healthCheckPath,
        appPort = this.appPort,
        startupTimeoutSeconds = this.startupTimeoutSeconds,
    )

    private fun BenchmarkConfig.toDto(files: Map<UUID, FileDto>) = GetBenchmarkQuery.LoadTestConfigDto(
        loadTestConfigId = this.id,
        openApiFile = files[this.openApiFileId]!!,
        behaviorModels = this.behaviorModels,
        operationalProfile = this.operationalProfile,
    )
}
