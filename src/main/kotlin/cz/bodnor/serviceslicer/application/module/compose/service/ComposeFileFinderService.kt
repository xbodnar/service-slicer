package cz.bodnor.serviceslicer.application.module.compose.service

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import cz.bodnor.serviceslicer.domain.compose.ComposeFile
import cz.bodnor.serviceslicer.domain.compose.ComposeFileRepository
import org.springframework.stereotype.Service

@Service
class ComposeFileFinderService(
    private val repository: ComposeFileRepository,
) : BaseFinderService<ComposeFile>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = ComposeFile::class
}
