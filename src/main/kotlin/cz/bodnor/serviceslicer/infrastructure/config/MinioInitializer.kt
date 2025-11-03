package cz.bodnor.serviceslicer.infrastructure.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener

@Configuration
@Profile("!test")
class MinioInitializer(
    private val minioClient: MinioClient,
    private val minioProperties: MinioProperties,
) {

    private val logger = KotlinLogging.logger {}

    @EventListener(ApplicationReadyEvent::class)
    fun initializeBucket() {
        try {
            val bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(minioProperties.bucketName)
                    .build(),
            )

            if (!bucketExists) {
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(minioProperties.bucketName)
                        .build(),
                )
                logger.info { "Created MinIO bucket: ${minioProperties.bucketName}" }
            } else {
                logger.info { "MinIO bucket already exists: ${minioProperties.bucketName}" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize MinIO bucket: ${minioProperties.bucketName}" }
            throw e
        }
    }
}
