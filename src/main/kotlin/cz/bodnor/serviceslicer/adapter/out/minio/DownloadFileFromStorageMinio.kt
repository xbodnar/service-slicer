package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadFileFromStorage
import cz.bodnor.serviceslicer.domain.file.File
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Component
class DownloadFileFromStorageMinio(
    private val minioConnector: MinioConnector,
) : DownloadFileFromStorage {

    override fun invoke(file: File): Path {
        val tempFile = Files.createTempFile(null, file.filename)

        minioConnector.downloadObject(file.storageKey).use { inputStream ->
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }

        return tempFile
    }
}
