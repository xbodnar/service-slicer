package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.io.Closeable
import java.net.ServerSocket

@Component
class SshTunnelManager(
    private val remoteExecutionProperties: RemoteExecutionProperties,
    private val localCommandExecutor: LocalCommandExecutor,
) {
    private val logger = KotlinLogging.logger {}

    data class SshTunnel(
        val localPort: Int,
        val remotePort: Int,
        val process: Process,
    ) : Closeable {
        override fun close() {
            process.destroy()
            process.waitFor()
        }
    }

    /**
     * Opens an SSH tunnel from a local port to the remote host:remotePort.
     * Returns the tunnel which must be closed after use.
     */
    fun openTunnel(remotePort: Int): SshTunnel {
        val localPort = findAvailablePort()

        logger.info {
            "Opening SSH tunnel: localhost:$localPort -> " +
                "${remoteExecutionProperties.host}:$remotePort via SSH"
        }

        val command = listOf(
            "ssh",
            "-N", // No remote command
            "-L", "0.0.0.0:$localPort:localhost:$remotePort", // Local port forwarding (bind to all interfaces)
            "-i", remoteExecutionProperties.sshKeyPath, // SSH key
            "-p", remoteExecutionProperties.port.toString(), // SSH port
            "-o", "StrictHostKeyChecking=no", // Auto-accept host key
            "-o", "UserKnownHostsFile=/dev/null", // Don't save to known_hosts
            "-o", "ServerAliveInterval=60", // Keep connection alive
            "-o", "ServerAliveCountMax=3",
            "${remoteExecutionProperties.username}@${remoteExecutionProperties.host}",
        )

        val processBuilder = ProcessBuilder(command)
            .redirectErrorStream(true)

        val process = processBuilder.start()

        // Wait a bit for the tunnel to establish
        Thread.sleep(2000)

        if (!process.isAlive) {
            val output = process.inputStream.bufferedReader().readText()
            throw RuntimeException("SSH tunnel failed to start. Output: $output")
        }

        logger.info {
            "SSH tunnel established successfully: localhost:$localPort -> ${remoteExecutionProperties.host}:$remotePort"
        }

        return SshTunnel(
            localPort = localPort,
            remotePort = remotePort,
            process = process,
        )
    }

    fun <T> withOptionalTunnel(
        remotePort: Int,
        block: (Int) -> T,
    ): T {
        if (remoteExecutionProperties.enabled) {
            openTunnel(remotePort).use { tunnel ->
                return block(tunnel.localPort)
            }
        } else {
            return block(remotePort)
        }
    }

    private fun findAvailablePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }
}
