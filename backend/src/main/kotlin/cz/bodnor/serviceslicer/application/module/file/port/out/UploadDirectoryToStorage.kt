package cz.bodnor.serviceslicer.application.module.file.port.out

import java.nio.file.Path

interface UploadDirectoryToStorage {

    operator fun invoke(
        directoryPath: Path,
        storageKey: String,
    )
}
