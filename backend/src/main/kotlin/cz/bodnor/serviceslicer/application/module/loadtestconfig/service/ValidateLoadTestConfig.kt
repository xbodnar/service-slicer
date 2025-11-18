package cz.bodnor.serviceslicer.application.module.loadtestconfig.service

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.LoadTestConfig
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalLoad
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import kotlin.math.roundToInt

object ValidateLoadTestConfig {

    operator fun invoke(
        loadTestConfig: LoadTestConfig,
        apiOperations: List<ApiOperation>,
    ) {
        if (loadTestConfig.behaviorModels.isNotEmpty()) {
            val sumOfUsageProfiles =
                (loadTestConfig.behaviorModels.sumOf { (it.usageProfile * 100).roundToInt() }) / 100.0
            verify(sumOfUsageProfiles == 1.0) {
                "Sum of behavior probabilities must be 1.0, but was $sumOfUsageProfiles"
            }
        }

        val sumOfFreq = loadTestConfig.operationalProfile.sumOf { (it.frequency * 100).roundToInt() } / 100.0
        verify(sumOfFreq == 1.0) {
            "Sum of load probabilities must be 1.0, but was $sumOfFreq"
        }

        // Validate that all ApiRequests in behavior models exist (at least the correct PATH and METHOD)
        verify(
            loadTestConfig.behaviorModels.flatMap { it.steps }.all { apiRequest ->
                apiOperations.any { op -> op.method == apiRequest.method && op.path == apiRequest.path }
            },
        ) {
            "Unknown operation ID in behavior model steps"
        }
    }
}
