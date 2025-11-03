package cz.bodnor.serviceslicer.domain.projectsource

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class ProjectSourceReadService(
    private val repository: ProjectSourceRepository,
) : BaseFinderService<ProjectSource>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = ProjectSource::class
}
