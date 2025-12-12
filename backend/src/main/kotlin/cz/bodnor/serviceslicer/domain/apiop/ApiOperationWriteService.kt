package cz.bodnor.serviceslicer.domain.apiop

import org.springframework.stereotype.Service

@Service
class ApiOperationWriteService(
    private val repository: ApiOperationRepository,
) {

    fun save(apiOperations: List<ApiOperation>) = repository.saveAll(apiOperations)
}
