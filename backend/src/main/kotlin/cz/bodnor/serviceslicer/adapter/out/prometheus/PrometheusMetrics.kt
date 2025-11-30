package cz.bodnor.serviceslicer.adapter.out.prometheus

import cz.bodnor.serviceslicer.domain.benchmarkrun.OperationId
import java.math.BigDecimal

data class PrometheusMetrics(
    val operationId: String,

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
