package cz.bodnor.serviceslicer.application.module.project.projectsource

import cz.bodnor.serviceslicer.application.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSourceRepository
import org.springframework.stereotype.Service

@Service
class GitHubProjectSourceFinderService(
    private val repository: GitHubProjectSourceRepository,
) : BaseFinderService<GitHubProjectSource>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = GitHubProjectSource::class
}
