package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.GenerateFileDownloadUrl
import org.springframework.stereotype.Component

@Component
class GenerateFileDownloadUrlMinio(
    private val minioConnector: MinioConnector,
) : GenerateFileDownloadUrl {

    override fun invoke(storageKey: String): String = minioConnector.getPresignedDownloadUrl(storageKey)
}
