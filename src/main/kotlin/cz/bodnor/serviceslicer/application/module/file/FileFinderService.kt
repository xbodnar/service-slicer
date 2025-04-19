package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.file.File
import cz.bodnor.serviceslicer.domain.file.FileRepository
import org.springframework.stereotype.Service

@Service
class FileFinderService(
    private val repository: FileRepository,
) : BaseFinderService<File>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = File::class
}
