package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.UploadDirectoryToStorage
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.streams.asSequence

@Component
class UploadDirectoryToStorageMinio(
    private val minioConnector: MinioConnector,
) : UploadDirectoryToStorage {

    override fun invoke(
        directoryPath: Path,
        storageKey: String,
    ) {
        Files.walk(directoryPath).asSequence()
            .filter { it.isRegularFile() }
            .forEach { file ->
                val relativePath = file.relativeTo(directoryPath)
                val storageKey = "$storageKey/$relativePath"

                minioConnector.uploadFile(file, storageKey)
            }
    }
}
