package cz.bodnor.serviceslicer.application.module.file.service

import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadDirectoryFromStorage
import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadFileFromStorage
import cz.bodnor.serviceslicer.domain.file.DirectoryReadService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively

@Service
class DiskOperations(
    private val fileReadService: FileReadService,
    private val directoryReadService: DirectoryReadService,
    private val downloadFileFromStorage: DownloadFileFromStorage,
    private val downloadDirectoryFromStorage: DownloadDirectoryFromStorage,
) {

    fun <T> withFile(
        fileId: UUID,
        block: (Path) -> T,
    ): T {
        var filePath: Path? = null
        try {
            val file = fileReadService.getById(fileId)
            filePath = downloadFileFromStorage(file)

            return block(filePath)
        } finally {
            filePath?.deleteIfExists()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    fun <T> withDirectory(
        directoryId: UUID,
        block: (Path) -> T,
    ): T {
        var dirPath: Path? = null
        try {
            val directory = directoryReadService.getById(directoryId)
            dirPath = downloadDirectoryFromStorage(directory.storageKey)

            return block(dirPath)
        } finally {
            dirPath?.deleteRecursively()
        }
    }
}
