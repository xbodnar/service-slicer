package cz.bodnor.serviceslicer.domain.testcase

import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal

@JvmInline value class OperationId(@get:JsonValue val value: String)

data class TargetTestCaseOperationMetrics(
    val operationId: OperationId,

    // counts
    val totalRequests: Long,
    val failedRequests: Long,

    // invocation frequency
    val invocationFrequency: BigDecimal,

    // latency
    val meanResponseTimeMs: BigDecimal,
    val stdDevResponseTimeMs: BigDecimal,

    // percentiles
    val p95DurationMs: BigDecimal,
    val p99DurationMs: BigDecimal,

    val passScalabilityThreshold: Boolean,
    val scalabilityShare: BigDecimal,
)

data class BaselineTestCaseOperationMetrics(
    val operationId: OperationId,

    // counts
    val totalRequests: Long,
    val failedRequests: Long,

    // latency
    val meanResponseTimeMs: BigDecimal,
    val stdDevResponseTimeMs: BigDecimal,

    // percentiles
    val p95DurationMs: BigDecimal,
    val p99DurationMs: BigDecimal,

    val scalabilityThreshold: BigDecimal,
)
