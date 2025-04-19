package cz.bodnor.serviceslicer

import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestHelperService(
    private val fileRepository: FileRepository,
) {
    fun getFile(
        id: UUID = UUID.randomUUID(),
        extension: String,
    ): File = fileRepository.save(
        File(
            id = id,
            extension = extension,
        ),
    )
}
