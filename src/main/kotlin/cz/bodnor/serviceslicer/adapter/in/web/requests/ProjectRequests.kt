package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectCommand
import java.util.UUID

data class CreateProjectRequest(
    val projectName: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val projectSourceId: UUID,
) {
    fun toCommand(): CreateProjectCommand = CreateProjectCommand(
        projectName = projectName,
        basePackageName = basePackageName,
        excludePackages = excludePackages,
        projectSourceId = projectSourceId,
    )
}
