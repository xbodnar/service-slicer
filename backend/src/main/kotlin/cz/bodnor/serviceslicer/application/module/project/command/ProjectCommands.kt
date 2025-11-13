package cz.bodnor.serviceslicer.application.module.project.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class CreateProjectCommand(
    val projectName: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val jarFileId: UUID,
    val projectDirId: UUID?,
) : Command<CreateProjectCommand.CreateProjectResult> {

    @Schema(name = "CreateProjectResult", description = "Result of creating a project")
    data class CreateProjectResult(
        @Schema(description = "ID of the created project")
        val projectId: UUID,
    )
}

data class InitializeProjectCommand(
    val projectId: UUID,
) : Command<Unit>
