package cz.bodnor.serviceslicer.application.module.file.port.out

import java.util.UUID

interface GetFileMetadataFromStorage {

    data class Result(
        val size: Long,
        val contentHash: String,
    )

    operator fun invoke(storageKey: String): Result
}
