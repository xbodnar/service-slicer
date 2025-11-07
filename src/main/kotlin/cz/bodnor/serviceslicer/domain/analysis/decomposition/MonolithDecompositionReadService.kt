package cz.bodnor.serviceslicer.domain.analysis.decomposition

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MonolithDecompositionReadService(
    private val repository: MonolithDecompositionRepository,
) : BaseFinderService<MonolithDecomposition>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = MonolithDecomposition::class

    fun findAllByProjectId(projectId: UUID) = repository.findByProjectId(projectId)
}
