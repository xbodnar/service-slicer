package cz.bodnor.serviceslicer.adapter.out.prometheus

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.benchmarkrun.OperationRunMetrics
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Component
class QueryLoadTestMetricsPrometheus(
    private val prometheusConnector: PrometheusConnector,
    private val objectMapper: ObjectMapper,
) : QueryLoadTestMetrics {

    private val logger = KotlinLogging.logger {}

    @Suppress("ktlint:standard:max-line-length")
    override fun invoke(
        benchmarkId: UUID,
        sutId: UUID,
        targetVus: Int,
        start: Instant,
        end: Instant,
    ): List<OperationRunMetrics> {
        logger.info { "Querying Prometheus for time range: start=$start, end=$end" }

        // Build label filter for k6 metrics
        val labelFilter = """benchmark_id="$benchmarkId",sut_id="$sutId",load="$targetVus""""
        logger.info { "Using label filter: $labelFilter" }

        val duration = end.epochSecond - start.epochSecond

        // Query 1: Get total requests count per operation using increase over the full duration
        val totalRequestsQuery = "sum by (operation, sut_id, load) (increase(k6_http_reqs_total{$labelFilter}[${duration}s]))"

        // Query 2: Get failed requests count per operation using increase over the full duration
        val failedRequestsQuery = "sum by (operation, sut_id, load) (increase(k6_http_reqs_total{$labelFilter,status!~\"2..\"}[${duration}s]))"

        // Query 3: Get mean response time histogram per operation
        val meanResponseTimeQuery = "histogram_avg(sum by (operation, sut_id, load) (rate(k6_http_req_duration_seconds{$labelFilter}[${duration}s])))"

        // Query 4: Standard deviation of response time
        val stdDevResponseTimeQuery = "histogram_stddev(sum by (operation, sut_id, load) (rate(k6_http_req_duration_seconds{$labelFilter}[${duration}s])))"

        // Query 5: 95th percentile response time
        val p95ResponseTimeQuery = "histogram_quantile(0.95, sum by (operation, sut_id, load) (rate(k6_http_req_duration_seconds{$labelFilter}[${duration}s])))"

        // Query 6: 99th percentile response time
        val p99ResponseTimeQuery = "histogram_quantile(0.99, sum by (operation, sut_id, load) (rate(k6_http_req_duration_seconds{$labelFilter}[${duration}s])))"

        // Execute instant queries at the end time with a lookback window to the start
        val totalRequests = prometheusConnector.query(totalRequestsQuery, end)
            .also { logger.debug { "Total requests response: $it" } }
            .parseVectorResult(sutId, targetVus)

        val failedRequests = prometheusConnector.query(failedRequestsQuery, end)
            .also { logger.debug { "Failed requests response: $it" } }
            .parseVectorResult(sutId, targetVus)

        val meanResponseTimeResponse = prometheusConnector.query(meanResponseTimeQuery, end)
            .also { logger.debug { "Mean response time response: $it" } }
            .parseVectorResult(sutId, targetVus)

        val stdDevResponseTimeResponse = prometheusConnector.query(stdDevResponseTimeQuery, end)
            .also { logger.debug { "StdDev response time response: $it" } }
            .parseVectorResult(sutId, targetVus)

        val p95ResponseTimeResponse = prometheusConnector.query(p95ResponseTimeQuery, end)
            .also { logger.debug { "P95 response time response: $it" } }
            .parseVectorResult(sutId, targetVus)

        val p99ResponseTimeResponse = prometheusConnector.query(p99ResponseTimeQuery, end)
            .also { logger.debug { "P99 response time response: $it" } }
            .parseVectorResult(sutId, targetVus)

        val allOperations = totalRequests.keys +
            failedRequests.keys +
            meanResponseTimeResponse.keys +
            stdDevResponseTimeResponse.keys +
            p95ResponseTimeResponse.keys +
            p99ResponseTimeResponse.keys

        return allOperations.map { op ->
            OperationRunMetrics(
                operationId = op,
                totalRequests = totalRequests[op]?.toLong() ?: 0L,
                failedRequests = failedRequests[op]?.toLong() ?: 0L,
                meanResponseTimeMs = meanResponseTimeResponse[op]?.multiply(BigDecimal(1000))!!,
                stdDevResponseTimeMs = stdDevResponseTimeResponse[op]?.multiply(BigDecimal(1000))!!,
                p95DurationMs = p95ResponseTimeResponse[op]?.multiply(BigDecimal(1000))!!,
                p99DurationMs = p99ResponseTimeResponse[op]?.multiply(BigDecimal(1000))!!,
            )
        }
    }

    private fun PrometheusResponse.parseVectorResult(
        sutId: UUID,
        targetVus: Int,
    ): Map<String, BigDecimal> {
        require(status == "success") {
            "Prometheus query returned non-success status: $status"
        }
        require(data != null) {
            "Prometheus query returned no data"
        }
        require(data.resultType == "vector") {
            "Expected vector result, but got ${data.resultType}"
        }

        return data.result.associate {
            require(it.metric.size == 3) {
                "Expected 3 labels, but got ${it.metric.size}"
            }
            require(it.metric["load"] == targetVus.toString()) {
                "Expected load=$targetVus, but got ${it.metric["load"]}"
            }
            require(it.metric["sut_id"] == sutId.toString()) {
                "Expected sut_id=$sutId, but got ${it.metric["sut_id"]}"
            }
            require(it.value?.size == 2) {
                "Expected 2 values, but got ${it.value?.size}"
            }

            val operation = it.metric["operation"] ?: error("Missing operation label in metric: ${it.metric}")
            val value = it.value[1] as String

            operation to BigDecimal(value)
        }
    }
}
