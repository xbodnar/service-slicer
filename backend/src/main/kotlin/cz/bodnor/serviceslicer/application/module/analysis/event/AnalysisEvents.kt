package cz.bodnor.serviceslicer.application.module.analysis.event

import java.util.UUID

data class AnalysisJobCreatedEvent(
    val analysisJobId: UUID,
)
