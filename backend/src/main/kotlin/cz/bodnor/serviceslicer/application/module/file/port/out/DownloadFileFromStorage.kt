package cz.bodnor.serviceslicer.application.module.file.port.out

import cz.bodnor.serviceslicer.domain.file.File
import java.nio.file.Path

interface DownloadFileFromStorage {

    operator fun invoke(file: File): Path
}
