package cz.bodnor.serviceslicer.application.module.project.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class CreateProjectFromZipCommand(
    val file: MultipartFile,
) : Command<CreateProjectFromZipCommand.Result> {

    data class Result(
        val projectId: UUID,
    )
}
