package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.GenerateFileUploadUrl
import io.minio.GetPresignedObjectUrlArgs
import io.minio.http.Method
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class GenerateFileUploadUrlMinio(
    private val minioConnector: MinioConnector,
) : GenerateFileUploadUrl {

    override fun invoke(storageKey: String): String = minioConnector.getPresignedObjectUrl(storageKey)
}
