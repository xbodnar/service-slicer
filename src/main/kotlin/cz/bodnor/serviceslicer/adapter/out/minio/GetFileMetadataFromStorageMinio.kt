package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.GetFileMetadataFromStorage
import cz.bodnor.serviceslicer.domain.file.FileReadService
import io.minio.StatObjectArgs
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class GetFileMetadataFromStorageMinio(
    private val minioConnector: MinioConnector,
) : GetFileMetadataFromStorage {

    override fun invoke(storageKey: String): GetFileMetadataFromStorage.Result {
        val objectStat = minioConnector.getObjectMetadata(storageKey)

        return GetFileMetadataFromStorage.Result(
            size = objectStat.size(),
            contentHash = objectStat.etag(),
        )
    }
}
