package cz.bodnor.serviceslicer.application.module.project.event

import java.util.UUID

data class ProjectCreatedEvent(
    val projectId: UUID,
)
