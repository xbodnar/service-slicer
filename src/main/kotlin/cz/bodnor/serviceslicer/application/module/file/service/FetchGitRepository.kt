package cz.bodnor.serviceslicer.application.module.file.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import org.springframework.stereotype.Component
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path

@Component
class FetchGitRepository {

    private val logger = KotlinLogging.logger {}

    private val loggerWriter = LoggerWriter(logger)

    operator fun invoke(
        uri: String,
        branch: String,
    ): Path {
        val destinationDir = Files.createTempDirectory("git-repo-")

        logger.info { "Cloning git repository..." }

        var git: Git? = null

        try {
            git = Git.cloneRepository()
                .setURI(uri)
                .setBranch(branch)
                .setDirectory(destinationDir.toFile())
                .setProgressMonitor(
                    TextProgressMonitor(loggerWriter),
                )
                .call()

            logger.info { "Git repository cloned" }

            return destinationDir
        } catch (e: Exception) {
            logger.error(e) { "Failed to clone git repository: $uri and branch: $branch" }
            throw e
        } finally {
            git?.close()
        }
    }
}

class LoggerWriter(
    private val logger: KLogger,
) : Writer() {

    private val buffer = StringBuilder()
    private val lockObj = Any()

    override fun write(
        cbuf: CharArray,
        off: Int,
        len: Int,
    ) {
        synchronized(lockObj) {
            val end = off + len
            var i = off
            while (i < end) {
                val c = cbuf[i++]
                if (c == '\n') {
                    emitLine()
                } else if (c != '\r') {
                    buffer.append(c)
                }
            }
        }
    }

    override fun flush() {
        synchronized(lockObj) {
            if (buffer.isNotEmpty()) {
                emitLine()
            }
        }
    }

    override fun close() = flush()

    private fun emitLine() {
        val msg = buffer.toString()
        buffer.setLength(0)

        logger.info { msg }
    }
}
