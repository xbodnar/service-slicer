package cz.bodnor.serviceslicer.application.module.operationalsetting.service

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.infrastructure.exception.applicationError
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import java.math.BigDecimal

object ValidateOperationalSetting {

    operator fun invoke(
        operationalSetting: OperationalSetting,
        apiOperations: List<ApiOperation>,
    ) {
        operationalSetting.operationalProfile.forEach { (load, frequency) ->
            verify(load > 0) { "Load must be greater than 0, but was $load" }
            verify(frequency > BigDecimal.ZERO) { "Frequency must be greater than 0, but was $frequency" }
        }

        operationalSetting.usageProfile.forEach { behaviorModel ->
            verify(behaviorModel.frequency > BigDecimal.ZERO) {
                "Frequency must be greater than 0, but was ${behaviorModel.frequency}"
            }
        }

        // Validate that probabilities sum to 1.0
        val sumOfOperationalProfile = operationalSetting.operationalProfile.values.sumOf { it }.stripTrailingZeros()
        verify(sumOfOperationalProfile == BigDecimal.ONE) {
            "Sum of load probabilities must be 1.0, but was $sumOfOperationalProfile"
        }

        val sumOfUsageProfile = operationalSetting.usageProfile.sumOf { it.frequency }.stripTrailingZeros()
        verify(sumOfUsageProfile == BigDecimal.ONE) {
            "Sum of behavior probabilities must be 1.0, but was $sumOfUsageProfile"
        }

        // Validate that all ApiRequests in behavior models exist (operationId, PATH and METHOD must match)
        val apiOperationById = apiOperations.associateBy { it.operationId }

        operationalSetting.usageProfile.flatMap { it.steps }.forEach { apiRequest ->
            val apiOperation = apiOperationById[apiRequest.operationId] ?: applicationError(
                "API Operation with ID ${apiRequest.operationId} not found in openApi " +
                    "file ${operationalSetting.openApiFile.id}",
            )

            verify(normalizePath(apiOperation.path) == normalizePath(apiRequest.path)) {
                "PATH does not match for operation '${apiOperation.operationId}', " +
                    "expected: ${apiOperation.path}, actual: ${apiRequest.path}"
            }
            verify(apiOperation.method == apiRequest.method) {
                "METHOD does not match for operation '${apiOperation.operationId}', " +
                    "expected: ${apiOperation.method}, actual: ${apiRequest.method}"
            }
        }
    }

    /**
     * Normalizes path by replacing path variables with a common placeholder.
     * - OpenAPI format: /articles/{slug} -> /articles/{var}
     * - User format: /articles/${articleSlug} -> /articles/{var}
     */
    private fun normalizePath(path: String): String {
        return path
            .replace(Regex("""\$\{[^}]+}"""), "{var}") // Replace ${varName} with {var}
            .replace(Regex("""\{[^}]+}"""), "{var}") // Replace {varName} with {var}
    }
}
