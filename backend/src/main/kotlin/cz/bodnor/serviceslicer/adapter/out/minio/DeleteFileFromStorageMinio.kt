package cz.bodnor.serviceslicer.adapter.out.minio

import cz.bodnor.serviceslicer.application.module.file.port.out.DeleteFileFromStorage
import org.springframework.stereotype.Component

@Component
class DeleteFileFromStorageMinio(
    private val minioConnector: MinioConnector,
) : DeleteFileFromStorage {

    override fun invoke(storageKey: String) {
        minioConnector.deleteObject(storageKey)
    }
}
