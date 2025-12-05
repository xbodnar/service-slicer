package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "k6")
data class K6Properties(
    /**
     * Docker image for k6.
     */
    val dockerImage: String = "ghcr.io/xbodnar/k6:latest",
    /**
     * Default test duration for k6 tests.
     */
    val testDuration: String = "1m",

    /**
     * Configuration for container network settings.
     *
     * To allow secure connection between K6 and the SUT, we use SSH tunneling.
     *
     * When running BE in Docker, we need to use `--network=container:<container-name>` to allow the container
     * to access the host network.
     *
     * When running BE natively, we need to use `--add-host=host.docker.internal:host-gateway` to allow the container
     * to access the host network.
     */
    val dockerNetworkConfig: DockerNetworkConfig? = null,
)

data class DockerNetworkConfig(
    val networkName: String,
    val containerName: String,
)
