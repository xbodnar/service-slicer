package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "k6")
data class K6Properties(
    /**
     * Docker image to use for k6 load tests.
     */
    val dockerImage: String = "grafana/k6:latest",

    /**
     * Directory where k6 test scripts are stored.
     */
    val scriptsDir: String = "data/k6-scripts",

    /**
     * Prometheus configuration for k6 metrics.
     */
    val prometheus: PrometheusConfig = PrometheusConfig(),
) {
    data class PrometheusConfig(
        /**
         * Prometheus remote write URL for k6 metrics.
         * If not set, k6 metrics will not be sent to Prometheus.
         * Example: http://localhost:9090/api/v1/write
         */
        val remoteWriteUrl: String? = null,
    )
}
