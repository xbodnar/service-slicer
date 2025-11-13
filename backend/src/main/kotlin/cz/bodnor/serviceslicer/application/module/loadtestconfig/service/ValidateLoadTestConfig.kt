package cz.bodnor.serviceslicer.application.module.loadtestconfig.service

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import kotlin.math.roundToInt

object ValidateLoadTestConfig {

    operator fun invoke(
        behaviorModels: List<BehaviorModel>,
        apiOperations: List<ApiOperation>,
        operationalProfile: OperationalProfile? = null,
    ) {
        if (behaviorModels.isNotEmpty()) {
            val sumOfUsageProfiles = (behaviorModels.sumOf { (it.usageProfile * 100).roundToInt() }) / 100.0
            verify(sumOfUsageProfiles == 1.0) {
                "Sum of behavior probabilities must be 1.0, but was $sumOfUsageProfiles"
            }
        }
        operationalProfile?.let {
            val sumOfFreq = operationalProfile.loadsToFreq.sumOf { (it.second * 100).roundToInt() } / 100.0
            verify(sumOfFreq == 1.0) {
                "Sum of load probabilities must be 1.0, but was $sumOfFreq"
            }
        }

        // Validate that all operation IDs in behavior models exist
        val operationToEntityMap = apiOperations.associateBy { it.name }
        verify(behaviorModels.flatMap { it.steps }.all { operationToEntityMap.containsKey(it) }) {
            "Unknown operation ID in behavior model steps"
        }
    }
}
