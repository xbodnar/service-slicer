package cz.bodnor.serviceslicer.application.module.project.service

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import org.springframework.stereotype.Service

@Service
class ProjectReadService(
    private val repository: ProjectRepository,
) : BaseFinderService<Project>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = Project::class
}
