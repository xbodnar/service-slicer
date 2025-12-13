package cz.bodnor.serviceslicer.domain.testsuite

import cz.bodnor.serviceslicer.domain.testcase.OperationId
import java.math.BigDecimal

data class TestSuiteResults(
    /**
     * The relative Domain Metric measures the overall scalability of a deployment architecture at a given load.
     * It represents the probability that the SUT does not fail under a given load.
     */
    val totalDomainMetric: BigDecimal,
    /**
     * Per operation experiment results
     */
    val operationExperimentResults: Map<OperationId, OperationExperimentResults>,
)

data class OperationExperimentResults(
    val operationId: String,
    val totalRequests: Long,
    val failedRequests: Long,

    val scalabilityFootprint: Int?,
    val scalabilityGap: BigDecimal?,
    val performanceOffset: BigDecimal?,
)
