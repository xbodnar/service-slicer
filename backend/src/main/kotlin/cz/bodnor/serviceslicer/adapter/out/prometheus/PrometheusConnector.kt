package cz.bodnor.serviceslicer.adapter.out.prometheus

import cz.bodnor.serviceslicer.infrastructure.config.PrometheusProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant

@Component
class PrometheusConnector(
    private val prometheusProperties: PrometheusProperties,
) {
    private val logger = KotlinLogging.logger {}

    private val webClient: WebClient = WebClient.builder()
        .baseUrl(prometheusProperties.queryUrl)
        .build()

    /**
     * Execute a PromQL query against Prometheus API.
     *
     * @param query The PromQL query string
     * @param time Optional timestamp for instant query (defaults to current time)
     * @return PrometheusResponse containing the query results
     */
    fun query(
        query: String,
        time: Instant? = null,
    ): PrometheusResponse {
        logger.debug { "Executing Prometheus query: $query" }

        // Use build(false) to skip strict URI validation since WebClient will properly encode query params
        val uri = UriComponentsBuilder
            .fromHttpUrl(prometheusProperties.queryUrl)
            .path("/api/v1/query")
            .queryParam("query", query)
            .apply { if (time != null) queryParam("time", time.epochSecond) }
            .build(false)
            .toUri()

        val response = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono<PrometheusResponse>()
            .block()
            ?: throw IllegalStateException("Empty response from Prometheus")

        logger.debug { "Prometheus query response: $response" }

        return response
    }

    /**
     * Execute a PromQL range query against Prometheus API.
     *
     * @param query The PromQL query string
     * @param start Start timestamp
     * @param end End timestamp
     * @param step Query resolution step (in seconds)
     * @return PrometheusResponse containing the query results
     */
    fun queryRange(
        query: String,
        start: Instant,
        end: Instant,
        step: String = "15s",
    ): PrometheusResponse {
        logger.debug { "Executing Prometheus range query: $query (start=$start, end=$end, step=$step)" }

        // Use build(false) to skip strict URI validation since WebClient will properly encode query params
        val uri = UriComponentsBuilder
            .fromHttpUrl(prometheusProperties.queryUrl)
            .path("/api/v1/query_range")
            .queryParam("query", query)
            .queryParam("start", start.epochSecond)
            .queryParam("end", end.epochSecond)
            .queryParam("step", step)
            .build(false)
            .toUri()

        val response = webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono<PrometheusResponse>()
            .block()
            ?: throw IllegalStateException("Empty response from Prometheus")

        logger.debug { "Prometheus range query response: $response" }

        return response
    }
}
