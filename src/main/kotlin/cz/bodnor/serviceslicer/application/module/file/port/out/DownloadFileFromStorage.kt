package cz.bodnor.serviceslicer.application.module.file.port.out

import java.nio.file.Path

interface DownloadFileFromStorage {

    operator fun invoke(
        storageKey: String,
        suffix: String? = null,
    ): Path
}
