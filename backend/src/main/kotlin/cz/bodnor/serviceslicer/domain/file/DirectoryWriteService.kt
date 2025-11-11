package cz.bodnor.serviceslicer.domain.file

import org.springframework.stereotype.Service

@Service
class DirectoryWriteService(
    private val repository: DirectoryRepository,
) {

    fun create(): Directory = repository.save(Directory())
}
