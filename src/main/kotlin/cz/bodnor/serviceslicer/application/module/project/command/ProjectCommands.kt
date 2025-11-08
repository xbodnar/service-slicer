package cz.bodnor.serviceslicer.application.module.project.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateProjectCommand(
    val projectName: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val jarFileId: UUID,
    val projectDirId: UUID?,
) : Command<CreateProjectCommand.CreateProjectResult> {

    data class CreateProjectResult(
        val projectId: UUID,
    )
}

data class InitializeProjectCommand(
    val projectId: UUID,
) : Command<Unit>
