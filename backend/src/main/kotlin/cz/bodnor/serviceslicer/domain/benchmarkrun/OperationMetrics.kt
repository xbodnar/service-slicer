package cz.bodnor.serviceslicer.domain.benchmarkrun

import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal

@JvmInline value class OperationId(@get:JsonValue val value: String)

data class OperationMetrics(
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
)
