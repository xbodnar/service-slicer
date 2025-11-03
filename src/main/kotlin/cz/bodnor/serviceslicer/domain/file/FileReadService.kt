package cz.bodnor.serviceslicer.domain.file

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class FileReadService(
    private val repository: FileRepository,
) : BaseFinderService<File>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = File::class
}
