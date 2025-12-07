package cz.bodnor.serviceslicer.domain.file

import org.springframework.stereotype.Service

@Service
class FileWriteService(
    private val repository: FileRepository,
) {

    fun create(
        filename: String,
        mimeType: String,
        expectedSize: Long,
    ): File = repository.save(
        File(
            filename = filename,
            mimeType = mimeType,
            fileSize = expectedSize,
        ),
    )
}
