package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.SocketException
import java.nio.file.Path
import java.time.Duration
import java.util.Properties
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.io.path.pathString

/**
 * Executes commands remotely via SSH on a dedicated test server.
 * Maintains a persistent SSH session with automatic reconnection and retry logic.
 */
@Service
class SshCommandExecutor(
    private val remoteProperties: RemoteExecutionProperties,
) : CommandExecutor {

    private val logger = KotlinLogging.logger {}
    private val jsch = JSch()

    private val sessionLock = ReentrantLock()
    private var persistentSession: Session? = null

    private val retryConfig = RetryConfig.custom<Any>()
        .maxAttempts(3)
        .waitDuration(Duration.ofSeconds(2))
        .retryExceptions(JSchException::class.java, SocketException::class.java)
        .build()

    private val connectionRetry = Retry.of("ssh-connection", retryConfig)

    init {
        if (remoteProperties.sshKeyPath.isNotEmpty()) {
            jsch.addIdentity(remoteProperties.sshKeyPath)
            logger.info { "Loaded SSH key from ${remoteProperties.sshKeyPath}" }
        }

        // Log retry events
        connectionRetry.eventPublisher.onRetry { event ->
            logger.warn {
                "SSH connection retry attempt ${event.numberOfRetryAttempts}: ${event.lastThrowable?.message}"
            }
        }
    }

    @PreDestroy
    fun cleanup() {
        sessionLock.withLock {
            persistentSession?.let { session ->
                if (session.isConnected) {
                    logger.info { "Closing persistent SSH session to ${remoteProperties.host}" }
                    session.disconnect()
                }
            }
            persistentSession = null
        }
    }

    override fun execute(
        command: List<String>,
        workingDirectory: File?,
    ): CommandExecutor.CommandResult {
        val commandString = buildCommandString(command, workingDirectory)
        logger.debug { "Executing remotely on ${remoteProperties.host}: $commandString" }

        return withSession { session ->
            val channel = session.openChannel("exec") as ChannelExec
            channel.setCommand(commandString)

            val outputStream = ByteArrayOutputStream()
            channel.outputStream = outputStream
            channel.setErrStream(outputStream)

            channel.connect()

            // Wait for command to complete
            while (!channel.isClosed) {
                Thread.sleep(100)
            }

            val exitCode = channel.exitStatus
            val output = outputStream.toString()

            channel.disconnect()

            CommandExecutor.CommandResult(exitCode, output)
        }
    }

    override fun transferFile(
        localFile: Path,
        remotePath: String,
    ): Path {
        val remotePath = "${remoteProperties.workDir}/$remotePath"
        logger.info { "Transferring file from $localFile to ${remoteProperties.host}:$remotePath" }

        withSession { session ->
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()

            try {
                // Create remote directory if it doesn't exist
                val remoteDir = remotePath.substringBeforeLast('/')
                createRemoteDirectory(channel, remoteDir)

                // Upload file
                channel.put(localFile.pathString, remotePath)
                logger.debug { "File transferred successfully to $remotePath" }
            } finally {
                channel.disconnect()
            }
        }

        return Path.of(remotePath)
    }

    override fun getTargetHost(): String = remoteProperties.host

    override fun getProjectWorkDir(projectName: String): File = File("${remoteProperties.workDir}/$projectName")

    /**
     * Executes the given block with a persistent SSH session.
     * The session is reused across multiple calls; only new channels are created per command.
     * Includes automatic reconnection with retry logic if the session is disconnected.
     */
    private fun <T> withSession(block: (Session) -> T): T = sessionLock.withLock {
        val session = getOrCreateSession()
        try {
            block(session)
        } catch (e: Exception) {
            // If the session appears to be broken, invalidate it for next call
            if (isSessionError(e)) {
                logger.warn { "SSH session error detected, will reconnect on next call: ${e.message}" }
                invalidateSession()
            }
            throw e
        }
    }

    /**
     * Gets the existing session or creates a new one with retry logic.
     */
    private fun getOrCreateSession(): Session {
        persistentSession?.let { session ->
            if (session.isConnected) {
                return session
            }
            logger.debug { "Existing session is disconnected, creating new one" }
        }

        // Create new session with retry
        val session = runBlocking {
            connectionRetry.executeSuspendFunction {
                createSession()
            }
        }

        persistentSession = session
        logger.info { "Established persistent SSH session to ${remoteProperties.host}" }
        return session
    }

    private fun createSession(): Session {
        val session = jsch.getSession(
            remoteProperties.username,
            remoteProperties.host,
            remoteProperties.port,
        )

        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        // Keep-alive to prevent idle disconnects
        config["ServerAliveInterval"] = "60"
        config["ServerAliveCountMax"] = "3"
        session.setConfig(config)

        session.connect(30_000) // 30 second connection timeout
        return session
    }

    private fun invalidateSession() {
        try {
            persistentSession?.disconnect()
        } catch (e: Exception) {
            logger.debug { "Error disconnecting invalid session: ${e.message}" }
        }
        persistentSession = null
    }

    private fun isSessionError(e: Exception): Boolean = when (e) {
        is JSchException ->
            e.message?.contains("session is down") == true ||
                e.message?.contains("channel is not opened") == true ||
                e.cause is SocketException

        is SocketException -> true

        else -> false
    }

    private fun buildCommandString(
        command: List<String>,
        workingDirectory: File?,
    ): String {
        val cmdString = command.joinToString(" ") { escapeShellArg(it) }
        return if (workingDirectory != null) {
            "cd ${escapeShellArg(workingDirectory.absolutePath)} && $cmdString"
        } else {
            cmdString
        }
    }

    private fun escapeShellArg(arg: String): String =
        if (arg.contains(" ") || arg.contains("$") || arg.contains("'") || arg.contains("\"")) {
            "'${arg.replace("'", "'\\''")}'"
        } else {
            arg
        }

    private fun createRemoteDirectory(
        channel: ChannelSftp,
        path: String,
    ) {
        val parts = path.split("/").filter { it.isNotEmpty() }
        var currentPath = if (path.startsWith("/")) "/" else ""

        for (part in parts) {
            currentPath += "$part/"
            try {
                channel.stat(currentPath)
            } catch (e: Exception) {
                // Directory doesn't exist, create it
                channel.mkdir(currentPath)
                logger.debug { "Created remote directory: $currentPath" }
            }
        }
    }
}
