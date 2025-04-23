package cz.bodnor.serviceslicer.application.module.project.projectsource

import cz.bodnor.serviceslicer.application.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProjectSourceFinderService(
    private val repository: ProjectSourceRepository,
) : BaseFinderService<ProjectSource>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = ProjectSource::class

    @Transactional(readOnly = true)
    fun getByProjectId(projectId: UUID): ProjectSource =
        repository.findByProjectId(projectId) ?: errorBlock("Project with id $projectId not found")
}
