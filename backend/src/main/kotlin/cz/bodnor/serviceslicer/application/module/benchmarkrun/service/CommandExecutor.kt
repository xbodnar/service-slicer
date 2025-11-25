package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import java.io.File
import java.nio.file.Path

/**
 * Interface for executing shell commands either locally or remotely via SSH.
 */
sealed interface CommandExecutor {
    /**
     * Result of a shell command execution.
     */
    data class CommandResult(val exitCode: Int, val output: String)

    /**
     * Execute a shell command.
     * @param command List of command parts (e.g., ["docker", "compose", "up"])
     * @param workingDirectory Working directory for the command (local or remote)
     * @return CommandResult containing exit code and output
     */
    fun execute(
        command: List<String>,
        workingDirectory: File?,
    ): CommandResult

    /**
     * Transfer a file to the execution environment (no-op for local, SCP for remote).
     * @param localFile File to transfer
     * @param remotePath Destination path in the execution environment
     * @return Path to the file in the execution environment
     */
    fun transferFile(
        localFile: Path,
        remotePath: String,
    ): Path

    /**
     * Get the hostname that should be used for health checks and connectivity.
     */
    fun getTargetHost(): String
}
