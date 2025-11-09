package cz.bodnor.serviceslicer.application.module.loadtestexperiment.service

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import cz.bodnor.serviceslicer.infrastructure.config.RemoteExecutionProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.pathString

/**
 * Executes commands remotely via SSH on a dedicated test server.
 */
@Service
@ConditionalOnProperty(name = ["loadtest.remote.enabled"], havingValue = "true")
class SshCommandExecutor(
    private val remoteProperties: RemoteExecutionProperties,
) : CommandExecutor {

    private val logger = KotlinLogging.logger {}
    private val jsch = JSch()

    init {
        if (remoteProperties.sshKeyPath.isNotEmpty()) {
            jsch.addIdentity(remoteProperties.sshKeyPath)
            logger.info { "Loaded SSH key from ${remoteProperties.sshKeyPath}" }
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

    private fun <T> withSession(block: (Session) -> T): T {
        val session = jsch.getSession(
            remoteProperties.username,
            remoteProperties.host,
            remoteProperties.port,
        )

        val config = Properties()
        config["StrictHostKeyChecking"] = "no" // For simplicity; in production, verify host keys
        session.setConfig(config)
        session.connect()

        return try {
            block(session)
        } finally {
            session.disconnect()
        }
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
