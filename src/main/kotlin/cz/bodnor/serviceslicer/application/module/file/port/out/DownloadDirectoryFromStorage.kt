package cz.bodnor.serviceslicer.application.module.file.port.out

import java.nio.file.Path

interface DownloadDirectoryFromStorage {

    operator fun invoke(storageKey: String): Path
}
