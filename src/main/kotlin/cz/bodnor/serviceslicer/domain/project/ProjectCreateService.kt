package cz.bodnor.serviceslicer.domain.project

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProjectCreateService(
    private val projectRepository: ProjectRepository,
) {

    @Transactional
    fun create(name: String) = projectRepository.save(Project(name = name))
}
