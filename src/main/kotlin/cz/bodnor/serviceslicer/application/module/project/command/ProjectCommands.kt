package cz.bodnor.serviceslicer.application.module.project.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class CreateProjectFromZipCommand(
    val file: MultipartFile,
    val projectName: String,
) : Command<CreateProjectFromZipCommand.Result> {

    data class Result(
        val projectId: UUID,
    )
}

data class CreateProjectFromGitHubCommand(
    val gitHubUrl: String,
    val projectName: String,
) : Command<CreateProjectFromGitHubCommand.Result> {

    data class Result(
        val projectId: UUID,
    )
}
