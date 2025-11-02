package cz.bodnor.serviceslicer.application.module.compose.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Path
import java.util.UUID

data class UploadComposeFileCommand(
    val projectId: UUID,
    val file: MultipartFile,
    val healthCheckUrl: String,
) : Command<UploadComposeFileCommand.Result> {

    data class Result(
        val composeFileId: UUID,
    )
}

data class RunAndValidateComposeCommand(
    val composeFilePath: Path,
    val healthCheckUrl: String,
    val startupTimeoutSeconds: Int,
) : Command<RunAndValidateComposeCommand.Result> {

    data class Result(
        val isHealthy: Boolean,
    )
}
