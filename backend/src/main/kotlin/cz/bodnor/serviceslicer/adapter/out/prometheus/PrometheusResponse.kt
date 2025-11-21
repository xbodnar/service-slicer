package cz.bodnor.serviceslicer.adapter.out.prometheus

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

/**
 * Prometheus API response wrapper.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PrometheusResponse(
    val status: String,
    val data: PrometheusData?,
)

/**
 * Prometheus response data containing result type and results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PrometheusData(
    val resultType: String,
    val result: List<PrometheusResult>,
)

/**
 * Single result from a Prometheus query.
 * Can contain either a simple value or a native histogram.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PrometheusResult(
    val metric: Map<String, String>,
    val value: List<Any>? = null, // [timestamp, "value"]
    val histogram: List<Any>? = null, // [timestamp, HistogramData]
)

/**
 * Native histogram data from Prometheus.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class PrometheusHistogram(
    val count: String,
    val sum: String,
    val buckets: List<List<Any>>? = null,
) {
    /**
     * Parse bucket data into typed HistogramBucket objects.
     * Bucket format: [bucket_index, lower_bound, upper_bound, count]
     */
    fun parsedBuckets(): List<HistogramBucket> = buckets?.mapNotNull { bucket ->
        if (bucket.size >= 4) {
            try {
                HistogramBucket(
                    index = bucket[0].toString().toIntOrNull() ?: 0,
                    lowerBound = bucket[1].toString().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    upperBound = bucket[2].toString().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    count = bucket[3].toString().toBigDecimalOrNull() ?: BigDecimal.ZERO,
                )
            } catch (e: Exception) {
                null // Skip malformed buckets
            }
        } else {
            null
        }
    } ?: emptyList()
}

/**
 * Represents a single bucket in a Prometheus native histogram.
 */
data class HistogramBucket(
    val index: Int,
    val lowerBound: BigDecimal,
    val upperBound: BigDecimal,
    val count: BigDecimal,
) {
    /**
     * Calculate the midpoint of this bucket for variance calculation.
     */
    val midpoint: BigDecimal
        get() = (lowerBound + upperBound) / BigDecimal(2)
}

/**
 * Parsed metric value with operation and component labels.
 */
data class ParsedMetric(
    val operationId: String,
    val component: String,
    val value: BigDecimal,
)
