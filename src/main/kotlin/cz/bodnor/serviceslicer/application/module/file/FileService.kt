package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileCreateService
import org.apache.tika.config.TikaConfig
import org.apache.tika.metadata.Metadata
import org.apache.tika.metadata.TikaCoreProperties
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Path
import java.util.UUID

@Service
class FileService(
    private val fileFinderService: FileFinderService,
    private val fileCreateService: FileCreateService,
    private val fileStorageService: FileStorageService,
) {

    private val tikaConfig = TikaConfig.getDefaultConfig()

    fun upload(multipartFile: MultipartFile): File = upload(multipartFile.inputStream, multipartFile.originalFilename!!)

    fun upload(
        inputStream: InputStream,
        filename: String,
    ): File {
        val bufferedInputStream = BufferedInputStream(inputStream)
        val fileExtension = getFileExtension(bufferedInputStream, filename)
        val file = fileCreateService.create(
            fileName = filename.removeSuffix(".$fileExtension"),
            extension = fileExtension,
        )

        fileStorageService.save(file.fileName(), bufferedInputStream)

        return file
    }

    fun get(fileId: UUID): Path {
        val file = fileFinderService.getById(id = fileId)

        return fileStorageService.get(file.fileName())
    }

    private fun getFileExtension(
        inputStream: InputStream,
        fileName: String?,
    ): String {
        val metadata = Metadata().apply {
            set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName)
        }

        val mediaType = tikaConfig.mimeRepository.detect(inputStream, metadata)
        val mimeType = tikaConfig.mimeRepository.forName(mediaType.toString())
        val extension = mimeType.extension?.removePrefix(".")

        require(!extension.isNullOrBlank())

        return extension
    }
}
