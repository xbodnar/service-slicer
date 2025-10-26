package cz.bodnor.serviceslicer

import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TestHelperService(
    private val projectRepository: ProjectRepository,
) {
    fun getProject(
        id: UUID = UUID.randomUUID(),
        name: String = "test",
        basePackageName: String = "cz.bodnor.serviceslicer",
        entityModifier: (Project) -> Unit = {},
    ): Project = projectRepository.save(
        Project(
            id = id,
            name = name,
            basePackageName = basePackageName,
        ).also(entityModifier),
    )
}
