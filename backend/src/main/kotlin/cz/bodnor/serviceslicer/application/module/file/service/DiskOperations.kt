package cz.bodnor.serviceslicer.application.module.file.service

import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadFileFromStorage
import cz.bodnor.serviceslicer.domain.file.FileReadService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.deleteIfExists

@Service
class DiskOperations(
    private val fileReadService: FileReadService,
    private val downloadFileFromStorage: DownloadFileFromStorage,
) {

    private val logger = KotlinLogging.logger {}

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
}
