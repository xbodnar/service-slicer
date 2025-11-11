package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadDirectoryFromStorage
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories

@Component
class DownloadDirectoryFromStorageMinio(
    private val minioConnector: MinioConnector,
) : DownloadDirectoryFromStorage {

    override fun invoke(storageKey: String): Path {
        val tempDir = Files.createTempDirectory("download-dir-")

        minioConnector.listObjects(storageKey).forEach { item ->
            val objectKey = item.objectName()
            val relativePath = objectKey.removePrefix(storageKey).removePrefix("/")

            if (relativePath.isNotEmpty()) {
                val targetPath = tempDir.resolve(relativePath)
                targetPath.parent?.createDirectories()

                minioConnector.downloadObject(objectKey).use { inputStream ->
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        return tempDir
    }
}
