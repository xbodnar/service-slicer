package cz.bodnor.serviceslicer

import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileRepository
import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import cz.bodnor.serviceslicer.domain.project.SourceType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestHelperService(
    private val fileRepository: FileRepository,
    private val projectRepository: ProjectRepository,
) {
    fun getFile(
        id: UUID = UUID.randomUUID(),
        fileName: String = "test",
        extension: String = "zip",
    ): File = fileRepository.save(
        File(
            id = id,
            originalFileName = fileName,
            extension = extension,
        ),
    )

    fun getProject(
        id: UUID = UUID.randomUUID(),
        name: String = "test",
        sourceType: SourceType = SourceType.ZIP_FILE,
        sourceFileId: UUID? = null,
        githubRepositoryUrl: String? = null,
        entityModifier: (Project) -> Unit = {},
    ): Project = projectRepository.save(
        Project(
            id = id,
            name = name,
            sourceType = sourceType,
            sourceFileId = sourceFileId,
            githubRepositoryUrl = githubRepositoryUrl,
        ).also(entityModifier),
    )
}
