package cz.bodnor.serviceslicer.application.module.benchmarkrun.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Path

/**
 * Executes commands locally on the same machine where the Spring Boot app is running.
 */
@Service
class LocalCommandExecutor : CommandExecutor {

    private val logger = KotlinLogging.logger {}

    override fun execute(
        command: List<String>,
        workingDirectory: File?,
    ): CommandExecutor.CommandResult {
        logger.debug { "Executing locally: ${command.joinToString(" ")}" }

        val pb = ProcessBuilder(command)
        if (workingDirectory != null) pb.directory(workingDirectory)
        pb.redirectErrorStream(true)

        val process = pb.start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        return CommandExecutor.CommandResult(exitCode, output)
    }

    override fun transferFile(
        localFile: Path,
        remotePath: String,
    ): Path {
        // For local execution, file is already in place
        return localFile
    }

    override fun getTargetHost(): String = "localhost"
}
