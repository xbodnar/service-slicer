package cz.bodnor.serviceslicer.application.module.file.port.out

interface GetFileMetadataFromStorage {

    data class Result(
        val size: Long,
        val contentHash: String,
    )

    operator fun invoke(storageKey: String): Result
}
