package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.infrastructure.config.MinioProperties
import io.minio.GetObjectArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.StatObjectArgs
import io.minio.StatObjectResponse
import io.minio.http.Method
import org.springframework.stereotype.Component
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.fileSize
import kotlin.text.toInt

@Component
class MinioConnector(
    private val minioProperties: MinioProperties,
) {

    private val minioClient: MinioClient = MinioClient.builder()
        .endpoint(minioProperties.endpoint)
        .credentials(minioProperties.accessKey, minioProperties.secretKey)
        .build()

    fun getPresignedObjectUrl(storageKey: String): String = minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.PUT)
            .bucket(minioProperties.bucketName)
            .`object`(storageKey)
            .expiry(minioProperties.presignedUrlExpiration.toInt(), TimeUnit.SECONDS)
            .build(),
    )

    fun getObjectMetadata(storageKey: String): StatObjectResponse = minioClient.statObject(
        StatObjectArgs.builder()
            .bucket(minioProperties.bucketName)
            .`object`(storageKey)
            .build(),
    )

    fun downloadObject(storageKey: String): InputStream = minioClient.getObject(
        GetObjectArgs.builder()
            .bucket(minioProperties.bucketName)
            .`object`(storageKey)
            .build(),
    )

    fun uploadFile(
        filePath: Path,
        storageKey: String,
    ) {
        Files.newInputStream(filePath).use { inputStream ->
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioProperties.bucketName)
                    .`object`(storageKey)
                    .stream(inputStream, filePath.fileSize(), -1)
                    .build(),
            )
        }
    }
}
