package cz.bodnor.serviceslicer.domain.analysis.suggestion

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MicroserviceSuggestionCreateService(
    private val repository: MicroserviceSuggestionRepository,
) {

    @Transactional
    fun save(suggestion: MicroserviceSuggestion): MicroserviceSuggestion = repository.save(suggestion)
}
