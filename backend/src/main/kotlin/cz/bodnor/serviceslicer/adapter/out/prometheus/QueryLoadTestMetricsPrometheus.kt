package cz.bodnor.serviceslicer.adapter.out.prometheus

import com.fasterxml.jackson.databind.ObjectMapper
import cz.bodnor.serviceslicer.application.module.benchmarkrun.out.QueryLoadTestMetrics
import cz.bodnor.serviceslicer.domain.testcase.OperationId
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
        testCaseId: UUID,
        start: Instant,
        end: Instant,
    ): List<QueryLoadTestMetrics.PerformanceMetrics> {
        // Build label filter for k6 metrics
        val labelFilter = """test_case_id="$testCaseId""""

        // k6 counters and histograms reset to 0 for each test run, so the raw value
        // at the end timestamp IS the total for this test. No need for increase().
        // We use the 'time' parameter to query at the specific end timestamp.

        // Query 1: Get total requests count per operation (raw counter value)
        val totalRequestsQuery = "sum by (operation) (k6_http_reqs_total{$labelFilter})"

        // Query 2: Get failed requests count per operation (raw counter value)
        val failedRequestsQuery = "sum by (operation) (k6_http_reqs_total{$labelFilter,status!~\"2..\"})"

        // Query 3-6: For native histograms, use the raw cumulative histogram at end time
        // k6 resets histograms for each test run, so the value at end IS the test total
        val histogramBase = "sum by (operation) (k6_http_req_duration_seconds{$labelFilter})"

        // Query 3: Get mean response time from native histogram per operation
        val meanResponseTimeQuery = "histogram_avg($histogramBase)"

        // Query 4: Standard deviation of response time from native histogram
        val stdDevResponseTimeQuery = "histogram_stddev($histogramBase)"

        // Query 5: 95th percentile response time from native histogram
        val p95ResponseTimeQuery = "histogram_quantile(0.95, $histogramBase)"

        // Query 6: 99th percentile response time from native histogram
        val p99ResponseTimeQuery = "histogram_quantile(0.99, $histogramBase)"

        // Execute instant queries at the end timestamp
        val totalRequests = prometheusConnector.query(totalRequestsQuery, end)
            .also { logger.debug { "Total requests response: $it" } }
            .parseVectorResult()

        val failedRequests = prometheusConnector.query(failedRequestsQuery, end)
            .also { logger.debug { "Failed requests response: $it" } }
            .parseVectorResult()

        val meanResponseTimeResponse = prometheusConnector.query(meanResponseTimeQuery, end)
            .also { logger.debug { "Mean response time response: $it" } }
            .parseVectorResult()

        val stdDevResponseTimeResponse = prometheusConnector.query(stdDevResponseTimeQuery, end)
            .also { logger.debug { "StdDev response time response: $it" } }
            .parseVectorResult()

        val p95ResponseTimeResponse = prometheusConnector.query(p95ResponseTimeQuery, end)
            .also { logger.debug { "P95 response time response: $it" } }
            .parseVectorResult()

        val p99ResponseTimeResponse = prometheusConnector.query(p99ResponseTimeQuery, end)
            .also { logger.debug { "P99 response time response: $it" } }
            .parseVectorResult()

        // Validate that we got at least some data
        require(totalRequests.isNotEmpty()) {
            "No metrics found in Prometheus for test case $testCaseId in time range [$start, $end]. " +
                "This indicates the test did not run properly or metrics were not written."
        }

        val allOperations = totalRequests.keys +
            failedRequests.keys +
            meanResponseTimeResponse.keys +
            stdDevResponseTimeResponse.keys +
            p95ResponseTimeResponse.keys +
            p99ResponseTimeResponse.keys

        return allOperations.map { op ->
            QueryLoadTestMetrics.PerformanceMetrics(
                operationId = OperationId(op),
                totalRequests = totalRequests[op]?.toLong() ?: 0L,
                failedRequests = failedRequests[op]?.toLong() ?: 0L,
                meanResponseTimeMs = (
                    meanResponseTimeResponse[op] ?: error("No mean response time returned from prometheus for $op")
                    ).multiply(BigDecimal(1000)),
                stdDevResponseTimeMs = (
                    stdDevResponseTimeResponse[op] ?: error("No std dev response time returned from prometheus for $op")
                    ).multiply(BigDecimal(1000)),
                p95DurationMs = (
                    p95ResponseTimeResponse[op] ?: error("No p95 response time returned from prometheus for $op")
                    ).multiply(BigDecimal(1000)),
                p99DurationMs = (
                    p99ResponseTimeResponse[op] ?: error("No p99 response time returned from prometheus for $op")
                    ).multiply(BigDecimal(1000)),
            )
        }
    }

    private fun PrometheusResponse.parseVectorResult(): Map<String, BigDecimal> {
        require(status == "success") {
            "Prometheus query returned non-success status: $status"
        }
        require(data != null) {
            "Prometheus query returned no data"
        }
        require(data.resultType == "vector") {
            "Expected vector result, but got ${data.resultType}"
        }

        return data.result
            .filter { it.metric.isNotEmpty() } // Filter out vector(0) fallback results with empty metric labels
            .associate {
                require(it.value?.size == 2) {
                    "Expected 2 values, but got ${it.value?.size}"
                }

                val operation = it.metric["operation"] ?: error("Missing operation label in metric: ${it.metric}")
                val value = it.value[1] as String

                operation to BigDecimal(value)
            }
    }
}
