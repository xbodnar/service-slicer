package cz.bodnor.serviceslicer.application.module.file

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.isRegularFile

@Service
class LocalFileStorageService(
    @Value("\${app.storage.local-path}") private val storagePath: String,
) : FileStorageService {

    override fun save(
        fileName: String,
        file: InputStream,
    ) {
        storagePath.ensureDirectoryExistsOrThrow()

        File(storagePath, fileName)
            .outputStream()
            .use { output -> file.use { it.copyTo(output) } }
    }

    override fun get(fileName: String): Path = Path.of(storagePath, fileName).also { assert(it.isRegularFile()) }

    private fun String.ensureDirectoryExistsOrThrow() = File(this).also {
        if (!it.exists() && !it.mkdirs()) throw Exception("The $this directory does not exist")
    }
}
