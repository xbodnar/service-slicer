package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.GenerateFileUploadUrl
import org.springframework.stereotype.Component

@Component
class GenerateFileUploadUrlMinio(
    private val minioConnector: MinioConnector,
) : GenerateFileUploadUrl {

    override fun invoke(storageKey: String): String = minioConnector.getPresignedObjectUrl(storageKey)
}
