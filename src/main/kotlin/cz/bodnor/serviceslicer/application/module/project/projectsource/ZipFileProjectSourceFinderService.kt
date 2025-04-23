package cz.bodnor.serviceslicer.application.module.project.projectsource

import cz.bodnor.serviceslicer.application.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSourceRepository
import org.springframework.stereotype.Service

@Service
class ZipFileProjectSourceFinderService(
    private val repository: ZipFileProjectSourceRepository,
) : BaseFinderService<ZipFileProjectSource>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = ZipFileProjectSource::class
}
