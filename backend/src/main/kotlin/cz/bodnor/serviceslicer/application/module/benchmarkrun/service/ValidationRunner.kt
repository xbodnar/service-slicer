package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmark.ValidationResult
import cz.bodnor.serviceslicer.domain.benchmark.ValidationState
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ValidationRunner(
    private val benchmarkReadService: BenchmarkReadService,
    private val sutReadService: SystemUnderTestReadService,
    private val sutRunner: SystemUnderTestRunner,
    private val k6Runner: K6Runner,
    private val k6Properties: K6Properties,
    private val objectMapper: ObjectMapper,
    private val resourceLoader: ResourceLoader,
    private val remoteExecutionProperties: RemoteExecutionProperties,
) {

    private val logger = KotlinLogging.logger {}

    fun runSutValidation(
        benchmarkId: UUID,
        systemUnderTestId: UUID,
    ): ValidationResult {
        logger.info { "Starting validation run for benchmark $benchmarkId and SUT $systemUnderTestId" }

        val benchmark = benchmarkReadService.getById(benchmarkId)
        val sut = sutReadService.getById(systemUnderTestId)

        try {
            // Start the SUT (blocking call - waits until SUT is healthy and ready)
            sutRunner.startSUT(sut)

            // Run validation test (no metrics, single iteration through all behavior models)
            val k6Result = k6Runner.runValidation(
                operationalSettingId = benchmark.operationalSetting.id,
                appPort = sut.dockerConfig.appPort,
            )

            return ValidationResult(
                validationState = ValidationState.VALID,
                k6Output = k6Result.output,
            )
        } catch (e: Exception) {
            logger.error(e) { "Validation run failed for benchmark $benchmarkId and SUT $systemUnderTestId" }
            return ValidationResult(
                validationState = ValidationState.INVALID,
                errorMessage = e.message ?: "Unknown error during validation",
            )
        } finally {
            logger.info { "Validation run finished, cleaning up..." }
            sutRunner.stopSUT()
        }
    }
}
