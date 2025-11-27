package cz.bodnor.serviceslicer.application.module.benchmark.service

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkConfig
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import kotlin.math.roundToInt

object ValidateLoadTestConfig {

    operator fun invoke(
        benchmarkConfig: BenchmarkConfig,
        apiOperations: List<ApiOperation>,
    ) {
        if (benchmarkConfig.behaviorModels.isNotEmpty()) {
            val sumOfUsageProfiles =
                (benchmarkConfig.behaviorModels.sumOf { (it.usageProfile * 100).roundToInt() }) / 100.0
            verify(sumOfUsageProfiles == 1.0) {
                "Sum of behavior probabilities must be 1.0, but was $sumOfUsageProfiles"
            }
        }

        val sumOfFreq = benchmarkConfig.operationalProfile.sumOf { (it.frequency * 100).roundToInt() } / 100.0
        verify(sumOfFreq == 1.0) {
            "Sum of load probabilities must be 1.0, but was $sumOfFreq"
        }

        // Validate that all ApiRequests in behavior models exist (at least the correct PATH and METHOD)
        verify(
            benchmarkConfig.behaviorModels.flatMap { it.steps }.all { apiRequest ->
                apiOperations.any { op -> op.method == apiRequest.method && op.path == apiRequest.path }
            },
        ) {
            "Unknown operation ID in behavior model steps"
        }
    }
}
