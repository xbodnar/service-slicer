package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.command.ExtractZipFileCommand
import cz.bodnor.serviceslicer.application.module.file.port.out.DownloadFileFromStorage
import cz.bodnor.serviceslicer.application.module.file.port.out.UploadDirectoryToStorage
import cz.bodnor.serviceslicer.application.module.file.service.UnzipFile
import cz.bodnor.serviceslicer.domain.file.DirectoryWriteService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively

@Component
class ExtractZipFileCommandHandler(
    private val fileReadService: FileReadService,
    private val directoryWriteService: DirectoryWriteService,
    private val downloadFileFromStorage: DownloadFileFromStorage,
    private val uploadDirectoryToStorage: UploadDirectoryToStorage,
    private val unzipFile: UnzipFile,
) :
    CommandHandler<ExtractZipFileCommand.Result, ExtractZipFileCommand> {

    override val command = ExtractZipFileCommand::class

    @OptIn(ExperimentalPathApi::class)
    @Transactional
    override fun handle(command: ExtractZipFileCommand): ExtractZipFileCommand.Result {
        val zipFile = fileReadService.getById(command.zipFileId)

        var zipPath: Path? = null
        var unzippedDir: Path? = null

        try {
            // Create directory to DB
            val directory = directoryWriteService.create()

            // 1. Download ZIP file to tmp dir
            zipPath = downloadFileFromStorage(zipFile.storageKey, ".zip")

            // 2. Unzip to tmp dir
            unzippedDir = Files.createTempDirectory("unzipped-${directory.id}")
            unzipFile(zipPath, unzippedDir)

            // 3. Upload unzipped folder to MinIO
            uploadDirectoryToStorage(unzippedDir, directory.storageKey)

            return ExtractZipFileCommand.Result(
                dirId = directory.id,
            )
        } finally {
            // 5. Delete zip and unzipped folder from tmp dir
            zipPath?.deleteIfExists()
            unzippedDir?.deleteRecursively()
        }
    }
}
