package cz.bodnor.serviceslicer.domain.compose

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ComposeFileCreateService(
    private val composeFileRepository: ComposeFileRepository,
) {

    @Transactional
    fun create(
        projectId: UUID,
        fileId: UUID,
        healthCheckUrl: String,
    ): ComposeFile = composeFileRepository.save(
        ComposeFile(
            id = UUID.randomUUID(),
            projectId = projectId,
            fileId = fileId,
            healthCheckUrl = healthCheckUrl,
        ),
    )
}
