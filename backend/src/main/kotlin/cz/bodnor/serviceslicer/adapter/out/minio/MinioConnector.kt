package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.infrastructure.config.MinioProperties
import io.minio.GetObjectArgs
import io.minio.ListObjectsArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.StatObjectResponse
import io.minio.messages.Item
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.fileSize
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Component
class MinioConnector(
    private val minioProperties: MinioProperties,
) {

    private val minioClient: MinioClient = MinioClient.builder()
        .endpoint(minioProperties.endpoint)
        .credentials(minioProperties.accessKey, minioProperties.secretKey)
        .build()

    // used only for presigning; does NOT need to be reachable from inside Docker
    private val s3Presigner = S3Presigner.builder()
        .endpointOverride(URI.create(minioProperties.externalEndpoint))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(minioProperties.accessKey, minioProperties.secretKey),
            ),
        )
        .region(Region.EU_CENTRAL_1)
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build()

    fun getPresignedObjectUrl(storageKey: String): String {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(minioProperties.bucketName)
            .key(storageKey)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(minioProperties.presignedUrlExpiration.seconds.toJavaDuration())
            .putObjectRequest(putObjectRequest)
            .build()

        return s3Presigner.presignPutObject(presignRequest).url().toString()
    }

    fun getPresignedDownloadUrl(storageKey: String): String {
        require(!storageKey.contains("..")) { "Invalid storage key" }

        val getObjectRequest = GetObjectRequest.builder()
            .bucket(minioProperties.bucketName)
            .key(storageKey)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(minioProperties.presignedUrlExpiration.toLong()))
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

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

    fun listObjects(prefix: String): Iterable<Item> = minioClient.listObjects(
        ListObjectsArgs.builder()
            .bucket(minioProperties.bucketName)
            .prefix(prefix)
            .recursive(true)
            .build(),
    ).map { it.get() }

    fun deleteObject(storageKey: String) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(minioProperties.bucketName)
                .`object`(storageKey)
                .build(),
        )
    }
}
