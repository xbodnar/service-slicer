package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "prometheus")
data class PrometheusProperties(
    /**
     * Prometheus remote write URL for k6 metrics.
     * If not set, k6 metrics will not be sent to Prometheus.
     * Example: http://localhost:9091/api/v1/write
     */
    val remoteWriteUrl: String? = null,

    /**
     * Prometheus query API URL for retrieving metrics.
     * Example: http://localhost:9091
     */
    val queryUrl: String = "http://localhost:9091",
)
