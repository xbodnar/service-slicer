package cz.bodnor.serviceslicer.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "loadtest.remote")
data class RemoteExecutionProperties(
    /**
     * Enable remote execution of load tests via SSH.
     */
    val enabled: Boolean = false,

    /**
     * Hostname or IP address of the remote server.
     */
    val host: String = "localhost",

    /**
     * SSH port (default: 22).
     */
    val port: Int = 22,

    /**
     * SSH username for authentication.
     */
    val username: String = "root",

    /**
     * Path to the SSH private key file on the local machine.
     */
    val sshKeyPath: String = "",

    /**
     * Working directory on the remote server for compose files.
     */
    val workDir: String = "/tmp/serviceslicer",
)
