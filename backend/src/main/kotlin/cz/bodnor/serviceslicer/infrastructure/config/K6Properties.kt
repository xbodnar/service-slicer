package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "k6")
data class K6Properties(
    val dockerImage: String = "ghcr.io/xbodnar/k6:latest",
    /**
     * Default test duration for k6 tests.
     */
    val testDuration: String = "1m",
)
