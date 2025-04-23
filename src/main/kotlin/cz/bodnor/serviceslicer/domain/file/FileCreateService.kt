package cz.bodnor.serviceslicer.domain.file

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FileCreateService(private val fileRepository: FileRepository) {

    @Transactional
    fun create(
        fileName: String,
        extension: String,
    ): File = fileRepository.save(File(id = UUID.randomUUID(), originalFileName = fileName, extension = extension))
}
