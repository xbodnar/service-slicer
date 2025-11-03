package cz.bodnor.serviceslicer.domain.projectsource

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectSourceWriteService(
    private val repository: ProjectSourceRepository,
) {

    fun create(
        jarFileId: UUID,
        projectDirId: UUID,
    ): ProjectSource = repository.save(
        ProjectSource(
            jarFileId = jarFileId,
        ).also { it.setProjectDirId(projectDirId) },
    )
}
