package cz.bodnor.serviceslicer.domain.analysis.graph

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ClassNodeReadService(
    private val repository: ClassNodeRepository,
) {

    fun findAllByProjectId(projectId: UUID) = repository.findAllByProjectId(projectId)
}
