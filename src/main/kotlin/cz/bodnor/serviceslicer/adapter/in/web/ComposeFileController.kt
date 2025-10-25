package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.application.module.compose.command.RunAndValidateComposeCommand
import cz.bodnor.serviceslicer.application.module.compose.command.UploadComposeFileCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/compose")
class ComposeFileController(
    private val commandBus: CommandBus,
) {

    @PostMapping("/upload")
    fun uploadComposeFile(
        @RequestPart file: MultipartFile,
        @RequestParam projectId: UUID,
        @RequestParam healthCheckUrl: String,
    ): UploadComposeFileCommand.Result = commandBus(
        UploadComposeFileCommand(
            projectId = projectId,
            file = file,
            healthCheckUrl = healthCheckUrl,
        ),
    )

    @PostMapping("/{composeFileId}/run")
    fun runAndValidate(@PathVariable composeFileId: UUID): RunAndValidateComposeCommand.Result = commandBus(
        RunAndValidateComposeCommand(
            composeFileId = composeFileId,
        ),
    )
}
