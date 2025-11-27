package cz.bodnor.serviceslicer.adapter.out.jooq

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.bodnor.serviceslicer.Tables.BENCHMARK
import cz.bodnor.serviceslicer.Tables.FILE
import cz.bodnor.serviceslicer.Tables.SYSTEM_UNDER_TEST
import cz.bodnor.serviceslicer.application.module.benchmark.port.out.GetBenchmark
import cz.bodnor.serviceslicer.application.module.benchmark.query.FileDto
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import cz.bodnor.serviceslicer.domain.sut.ValidationResult
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
            )
            .from(BENCHMARK)
            .where(BENCHMARK.ID.eq(benchmarkId))
            .fetchOne() ?: error("Benchmark with id: $benchmarkId not found!")

        val benchmarkConfig = objectMapper.readValue<BenchmarkConfig>(
            benchmarkRecord[BENCHMARK.CONFIG]!!.data(),
        )

        val suts = dsl.select(
            SYSTEM_UNDER_TEST.ID,
            SYSTEM_UNDER_TEST.NAME,
            SYSTEM_UNDER_TEST.DESCRIPTION,
            SYSTEM_UNDER_TEST.IS_BASELINE,
            SYSTEM_UNDER_TEST.DOCKER_CONFIG,
            SYSTEM_UNDER_TEST.DATABASE_SEED_CONFIG,
            SYSTEM_UNDER_TEST.VALIDATION_RESULT,
        ).from(SYSTEM_UNDER_TEST)
            .where(SYSTEM_UNDER_TEST.BENCHMARK_ID.eq(benchmarkId))
            .fetch()

        val fileIds = suts.flatMap { row ->
            val dockerConfigFileId = objectMapper.readValue<DockerConfig>(
                row[SYSTEM_UNDER_TEST.DOCKER_CONFIG]!!.data(),
            ).composeFileId

            val databaseSeedConfigFileId = row[SYSTEM_UNDER_TEST.DATABASE_SEED_CONFIG]?.let {
                objectMapper.readValue<DatabaseSeedConfig>(it.data())
            }?.sqlSeedFileId

            listOfNotNull(dockerConfigFileId, databaseSeedConfigFileId)
        }.toSet() +
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

        val sutDtos = suts.map {
            GetBenchmarkQuery.SystemUnderTestDto(
                systemUnderTestId = it.get(SYSTEM_UNDER_TEST.ID)!!,
                name = it.get(SYSTEM_UNDER_TEST.NAME)!!,
                description = it.get(SYSTEM_UNDER_TEST.DESCRIPTION),
                isBaseline = it.get(SYSTEM_UNDER_TEST.IS_BASELINE)!!,
                dockerConfig = objectMapper.readValue<DockerConfig>(
                    it.get(SYSTEM_UNDER_TEST.DOCKER_CONFIG)!!.data(),
                ).let { dockerConfig ->
                    GetBenchmarkQuery.DockerConfigDto(
                        composeFile = files.find { it.fileId == dockerConfig.composeFileId }!!,
                        healthCheckPath = dockerConfig.healthCheckPath,
                        appPort = dockerConfig.appPort,
                        startupTimeoutSeconds = dockerConfig.startupTimeoutSeconds,
                    )
                },
                databaseSeedConfig = it.get(SYSTEM_UNDER_TEST.DATABASE_SEED_CONFIG)?.let { databaseSeedConfig ->
                    objectMapper.readValue<DatabaseSeedConfig>(
                        databaseSeedConfig.data(),
                    ).let { databaseSeedConfig ->
                        GetBenchmarkQuery.DatabaseSeedConfigDto(
                            sqlSeedFile = files.find { it.fileId == databaseSeedConfig.sqlSeedFileId }!!,
                            dbContainerName = databaseSeedConfig.dbContainerName,
                            dbPort = databaseSeedConfig.dbPort,
                            dbName = databaseSeedConfig.dbName,
                            dbUsername = databaseSeedConfig.dbUsername,
                        )
                    }
                },
                validationResult = it.get(SYSTEM_UNDER_TEST.VALIDATION_RESULT)?.let { validationResult ->
                    objectMapper.readValue<ValidationResult>(validationResult.data())
                },
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
            systemsUnderTest = sutDtos,
            createdAt = benchmarkRecord.get(BENCHMARK.CREATED_AT)!!,
            updatedAt = benchmarkRecord.get(BENCHMARK.UPDATED_AT)!!,
        )
    }
}
