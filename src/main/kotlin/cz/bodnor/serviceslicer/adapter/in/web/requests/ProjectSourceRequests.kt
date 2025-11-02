package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.projectsource.command.CreateGitProjectSourceCommand
import cz.bodnor.serviceslicer.application.module.projectsource.command.CreateZipProjectSourceCommand
import java.util.UUID

data class CreateZipProjectSourceRequest(
    val jarFileId: UUID,
    val zipFileId: UUID,
) {
    fun toCommand(): CreateZipProjectSourceCommand = CreateZipProjectSourceCommand(
        jarFileId = jarFileId,
        zipFileId = zipFileId,
    )
}

data class CreateGitProjectSourceRequest(
    val jarFileId: UUID,
    val repositoryUrl: String,
    val branch: String,
) {
    fun toCommand(): CreateGitProjectSourceCommand = CreateGitProjectSourceCommand(
        jarFileId = jarFileId,
        repositoryUrl = repositoryUrl,
        branch = branch,
    )
}
