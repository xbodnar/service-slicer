package cz.bodnor.serviceslicer.domain.project

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ProjectWriteService(
    private val repository: ProjectRepository,
) {

    fun create(
        name: String,
        basePackageName: String,
        excludePackages: List<String>,
        jarFileId: UUID,
        projectDirId: UUID?,
    ): Project = repository.save(
        Project(
            name = name,
            basePackageName = basePackageName,
            excludePackages = excludePackages,
            jarFileId = jarFileId,
            projectDirId = projectDirId,
        ),
    )
}
