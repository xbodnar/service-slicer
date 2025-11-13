package cz.bodnor.serviceslicer.domain.file

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FileReadService(
    private val repository: FileRepository,
) : BaseFinderService<File>(repository) {

    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = File::class

    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): Page<File> = repository.findAll(pageable)
}
