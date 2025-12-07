package cz.bodnor.serviceslicer.domain.decomposition

import org.springframework.stereotype.Service

@Service
class MonolithArtifactWriteService(
    private val repository: MonolithArtifactRepository,
) {

    fun create(artifact: MonolithArtifact) = repository.save(artifact)
}
