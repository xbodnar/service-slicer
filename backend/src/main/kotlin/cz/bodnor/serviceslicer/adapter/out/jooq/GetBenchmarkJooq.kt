package cz.bodnor.serviceslicer.adapter.out.jooq

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.bodnor.serviceslicer.Tables.BENCHMARK
import cz.bodnor.serviceslicer.Tables.FILE
import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GetBenchmark
import cz.bodnor.serviceslicer.application.module.benchmark.query.FileDto
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.benchmark.SystemUnderTest
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetBenchmarkJooq(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : GetBenchmark {

    override fun invoke(benchmarkId: UUID): GetBenchmarkQuery.Result {
        val benchmarkRecord = dsl
            .select(
                BENCHMARK.ID,
                BENCHMARK.NAME,
                BENCHMARK.DESCRIPTION,
                BENCHMARK.CREATED_AT,
                BENCHMARK.UPDATED_AT,
                BENCHMARK.CONFIG,
                BENCHMARK.SYSTEMS_UNDER_TEST,
            )
            .from(BENCHMARK)
            .where(BENCHMARK.ID.eq(benchmarkId))
            .fetchOne() ?: error("Benchmark with id: $benchmarkId not found!")

        val benchmarkConfig = objectMapper.readValue<BenchmarkConfig>(
            benchmarkRecord[BENCHMARK.CONFIG]!!.data(),
        )
        val suts = objectMapper.readValue<List<SystemUnderTest>>(
            benchmarkRecord[BENCHMARK.SYSTEMS_UNDER_TEST]!!.data(),
        )

        val fileIds =
            suts.map { it.dockerConfig.composeFileId }.toSet() +
                suts.mapNotNull { it.databaseSeedConfig?.sqlSeedFileId } +
                benchmarkConfig.openApiFileId

        val files = dsl.select(
            FILE.ID,
            FILE.FILENAME,
            FILE.MIME_TYPE,
            FILE.EXPECTED_SIZE,
        )
            .from(FILE)
            .where(FILE.ID.`in`(fileIds))
            .fetch()
            .map { row ->
                FileDto(
                    fileId = row.get(FILE.ID)!!,
                    filename = row.get(FILE.FILENAME)!!,
                    fileSize = row.get(FILE.EXPECTED_SIZE)!!,
                )
            }

        return GetBenchmarkQuery.Result(
            benchmarkId = benchmarkRecord.get(BENCHMARK.ID)!!,
            name = benchmarkRecord.get(BENCHMARK.NAME)!!,
            description = benchmarkRecord.get(BENCHMARK.DESCRIPTION),
            loadTestConfig = GetBenchmarkQuery.LoadTestConfigDto(
                loadTestConfigId = benchmarkConfig.id,
                openApiFile = files.find { it.fileId == benchmarkConfig.openApiFileId }!!,
                behaviorModels = benchmarkConfig.behaviorModels,
                operationalProfile = benchmarkConfig.operationalProfile,
            ),
            systemsUnderTest = suts.map { sut ->
                GetBenchmarkQuery.SystemUnderTestDto(
                    systemUnderTestId = sut.id,
                    name = sut.name,
                    description = sut.description,
                    isBaseline = sut.isBaseline,
                    dockerConfig = GetBenchmarkQuery.DockerConfigDto(
                        composeFile = files.find { it.fileId == sut.dockerConfig.composeFileId }!!,
                        healthCheckPath = sut.dockerConfig.healthCheckPath,
                        appPort = sut.dockerConfig.appPort,
                        startupTimeoutSeconds = sut.dockerConfig.startupTimeoutSeconds,
                    ),
                    databaseSeedConfig = sut.databaseSeedConfig?.let {
                        GetBenchmarkQuery.DatabaseSeedConfigDto(
                            sqlSeedFile = files.find { file -> file.fileId == it.sqlSeedFileId }!!,
                            dbContainerName = it.dbContainerName,
                            dbPort = it.dbPort,
                            dbName = it.dbName,
                            dbUsername = it.dbUsername,
                        )
                    },
                    validationResult = sut.validationResult,
                )
            },
            createdAt = benchmarkRecord.get(BENCHMARK.CREATED_AT)!!,
            updatedAt = benchmarkRecord.get(BENCHMARK.UPDATED_AT)!!,
        )
    }
}
