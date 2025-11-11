package cz.bodnor.serviceslicer.domain.file

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class DirectoryReadService(
    private val repository: DirectoryRepository,
) : BaseFinderService<Directory>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = Directory::class
}
