package cz.bodnor.serviceslicer.application.module.loadtestconfig.service

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import cz.bodnor.serviceslicer.domain.loadtestconfig.OperationalProfile

object ValidateLoadTestConfig {

    operator fun invoke(
        behaviorModels: List<BehaviorModel>,
        apiOperations: List<ApiOperation>,
        operationalProfile: OperationalProfile? = null,
    ) {
        if (behaviorModels.isNotEmpty()) {
            require(behaviorModels.sumOf { it.usageProfile } == 1.0) {
                "Sum of behavior probabilities must be 1.0"
            }
        }
        operationalProfile?.let {
            require(operationalProfile.freq.sumOf { it } == 1.0) {
                "Sum of load probabilities must be 1.0"
            }
        }

        // Validate that all operation IDs in behavior models exist
        val operationToEntityMap = apiOperations.associateBy { it.name }
        require(behaviorModels.flatMap { it.steps }.all { operationToEntityMap.containsKey(it) }) {
            "Unknown operation ID in behavior model steps"
        }
    }
}
