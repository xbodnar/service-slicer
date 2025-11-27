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
     * Default test duration for k6 tests.
     */
    val testDuration: String = "30s",
)
