package cz.bodnor.serviceslicer.adapter.out.jooq

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import cz.bodnor.serviceslicer.Tables.FILE
import cz.bodnor.serviceslicer.Tables.LOAD_TEST_CONFIG
import cz.bodnor.serviceslicer.Tables.LOAD_TEST_EXPERIMENT
import cz.bodnor.serviceslicer.Tables.SYSTEM_UNDER_TEST
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.port.out.GetLoadTestExperiment
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.FileDto
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.GetLoadTestExperimentQuery
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetLoadTestExperimentJooq(
    private val dsl: DSLContext,
    private val objectMapper: ObjectMapper,
) : GetLoadTestExperiment {

    override fun invoke(experimentId: UUID): GetLoadTestExperimentQuery.Result {
        // Fetch experiment with load test config and openapi file
        val openApiFileAlias = FILE.`as`("openapi_file")

        val experimentRecord = dsl
            .select(
                LOAD_TEST_EXPERIMENT.ID,
                LOAD_TEST_EXPERIMENT.NAME,
                LOAD_TEST_EXPERIMENT.DESCRIPTION,
                LOAD_TEST_EXPERIMENT.CREATED_AT,
                LOAD_TEST_EXPERIMENT.UPDATED_AT,
                LOAD_TEST_CONFIG.ID.`as`("config_id"),
                LOAD_TEST_CONFIG.BEHAVIOR_MODELS,
                LOAD_TEST_CONFIG.OPERATIONAL_PROFILE,
                openApiFileAlias.ID.`as`("openapi_file_id"),
                openApiFileAlias.FILENAME.`as`("openapi_filename"),
                openApiFileAlias.EXPECTED_SIZE.`as`("openapi_file_size"),
            )
            .from(LOAD_TEST_EXPERIMENT)
            .join(LOAD_TEST_CONFIG).on(LOAD_TEST_EXPERIMENT.LOAD_TEST_CONFIG_ID.eq(LOAD_TEST_CONFIG.ID))
            .join(openApiFileAlias).on(LOAD_TEST_CONFIG.OPEN_API_FILE_ID.eq(openApiFileAlias.ID))
            .where(LOAD_TEST_EXPERIMENT.ID.eq(experimentId))
            .fetchOne() ?: error("LoadTestExperiment with id: $experimentId not found!")

        // Fetch systems under test with their compose and jar files
        val composeFileAlias = FILE.`as`("compose_file")
        val jarFileAlias = FILE.`as`("jar_file")

        val sutRecords = dsl
            .select(
                SYSTEM_UNDER_TEST.ID,
                SYSTEM_UNDER_TEST.NAME,
                SYSTEM_UNDER_TEST.DESCRIPTION,
                SYSTEM_UNDER_TEST.HEALTH_CHECK_PATH,
                SYSTEM_UNDER_TEST.APP_PORT,
                SYSTEM_UNDER_TEST.STARTUP_TIMEOUT_SECONDS,
                composeFileAlias.ID.`as`("compose_file_id"),
                composeFileAlias.FILENAME.`as`("compose_filename"),
                composeFileAlias.EXPECTED_SIZE.`as`("compose_file_size"),
                jarFileAlias.ID.`as`("jar_file_id"),
                jarFileAlias.FILENAME.`as`("jar_filename"),
                jarFileAlias.EXPECTED_SIZE.`as`("jar_file_size"),
            )
            .from(SYSTEM_UNDER_TEST)
            .join(composeFileAlias).on(SYSTEM_UNDER_TEST.COMPOSE_FILE_ID.eq(composeFileAlias.ID))
            .join(jarFileAlias).on(SYSTEM_UNDER_TEST.JAR_FILE_ID.eq(jarFileAlias.ID))
            .where(SYSTEM_UNDER_TEST.EXPERIMENT_ID.eq(experimentId))
            .fetch()

        // Parse JSON fields
        val behaviorModels = experimentRecord.get(LOAD_TEST_CONFIG.BEHAVIOR_MODELS)?.data()?.let {
            objectMapper.readValue<List<BehaviorModel>>(it)
        } ?: emptyList()

        val operationalProfile = experimentRecord.get(LOAD_TEST_CONFIG.OPERATIONAL_PROFILE)?.data()?.let {
            objectMapper.readValue<OperationalProfile>(it)
        }

        return GetLoadTestExperimentQuery.Result(
            experimentId = experimentRecord.get(LOAD_TEST_EXPERIMENT.ID)!!,
            name = experimentRecord.get(LOAD_TEST_EXPERIMENT.NAME)!!,
            description = experimentRecord.get(LOAD_TEST_EXPERIMENT.DESCRIPTION),
            loadTestConfig = GetLoadTestExperimentQuery.LoadTestConfigDto(
                loadTestConfigId = experimentRecord.get("config_id", UUID::class.java)!!,
                openApiFile = FileDto(
                    fileId = experimentRecord.get("openapi_file_id", UUID::class.java)!!,
                    filename = experimentRecord.get("openapi_filename", String::class.java)!!,
                    fileSize = experimentRecord.get("openapi_file_size", Long::class.java)!!,
                ),
                behaviorModels = behaviorModels,
                operationalProfile = operationalProfile,
            ),
            systemsUnderTest = sutRecords.map { sut ->
                GetLoadTestExperimentQuery.SystemUnderTestDto(
                    systemUnderTestId = sut.get(SYSTEM_UNDER_TEST.ID)!!,
                    name = sut.get(SYSTEM_UNDER_TEST.NAME)!!,
                    composeFile = FileDto(
                        fileId = sut.get("compose_file_id", UUID::class.java)!!,
                        filename = sut.get("compose_filename", String::class.java)!!,
                        fileSize = sut.get("compose_file_size", Long::class.java)!!,
                    ),
                    jarFile = FileDto(
                        fileId = sut.get("jar_file_id", UUID::class.java)!!,
                        filename = sut.get("jar_filename", String::class.java)!!,
                        fileSize = sut.get("jar_file_size", Long::class.java)!!,
                    ),
                    description = sut.get(SYSTEM_UNDER_TEST.DESCRIPTION),
                    healthCheckPath = sut.get(SYSTEM_UNDER_TEST.HEALTH_CHECK_PATH)!!,
                    appPort = sut.get(SYSTEM_UNDER_TEST.APP_PORT)!!,
                    startupTimeoutSeconds = sut.get(SYSTEM_UNDER_TEST.STARTUP_TIMEOUT_SECONDS)!!,
                )
            },
            createdAt = experimentRecord.get(LOAD_TEST_EXPERIMENT.CREATED_AT)!!,
            updatedAt = experimentRecord.get(LOAD_TEST_EXPERIMENT.UPDATED_AT)!!,
        )
    }
}
