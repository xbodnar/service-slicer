package cz.bodnor.serviceslicer.application.module.decomposition.event

import java.util.UUID

data class DecompositionJobCreatedEvent(
    val decompositionJobId: UUID,
)
