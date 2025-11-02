package cz.bodnor.serviceslicer.application.module.projectsource.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateZipProjectSourceCommand(
    val jarFileId: UUID,
    val zipFileId: UUID,
) : Command<CreateZipProjectSourceCommand.Result> {

    data class Result(
        val projectSourceId: UUID,
    )
}

data class CreateGitProjectSourceCommand(
    val jarFileId: UUID,
    val repositoryUrl: String,
    val branch: String,
) : Command<CreateGitProjectSourceCommand.Result> {

    data class Result(
        val projectSourceId: UUID,
    )
}
