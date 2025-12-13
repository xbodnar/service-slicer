package cz.bodnor.serviceslicer.application.module.benchmark

import cz.bodnor.serviceslicer.adapter.`in`.web.benchmark.BenchmarkSystemUnderTestDto
import cz.bodnor.serviceslicer.adapter.`in`.web.sut.SystemUnderTestMapper
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GetBenchmarkQueryHandler(
    private val benchmarkReadService: BenchmarkReadService,
    private val fileReadService: FileReadService,
    private val sutMapper: SystemUnderTestMapper,
) : QueryHandler<GetBenchmarkQuery.Result, GetBenchmarkQuery> {
    override val query = GetBenchmarkQuery::class

    @Transactional(readOnly = true)
    override fun handle(query: GetBenchmarkQuery): GetBenchmarkQuery.Result {
        val benchmark = benchmarkReadService.getById(query.benchmarkId)
        val fileIds = benchmark.systemsUnderTest.flatMap { it.systemUnderTest.getFileIds() }.toSet()
        val files = fileReadService.findAllByIds(fileIds).associateBy { it.id }

        val systemsUnderTest = benchmark.systemsUnderTest.map {
            BenchmarkSystemUnderTestDto(
                id = it.systemUnderTest.id,
                createdAt = it.systemUnderTest.createdAt,
                updatedAt = it.systemUnderTest.updatedAt,
                name = it.systemUnderTest.name,
                description = it.systemUnderTest.description,
                dockerConfig = sutMapper.toDto(
                    it.systemUnderTest.dockerConfig.withFile(files[it.systemUnderTest.dockerConfig.composeFileId]!!),
                ),
                databaseSeedConfigs = it.systemUnderTest.databaseSeedConfigs.map { dbConfig ->
                    sutMapper.toDto(dbConfig.withFile(files[dbConfig.sqlSeedFileId]!!))
                },
                isBaseline = it.isBaseline,
                validationResult = it.validationResult,
            )
        }

        return GetBenchmarkQuery.Result(
            benchmark = benchmark,
            systemsUnderTest = systemsUnderTest,
        )
    }
}
