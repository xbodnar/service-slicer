package cz.bodnor.serviceslicer.domain.file

import org.springframework.stereotype.Service
import java.util.UUID

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
            id = UUID.randomUUID(),
            filename = filename,
            mimeType = mimeType,
            expectedSize = expectedSize,
        ),
    )
}
