package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.FetchGitRepositoryRequest
import cz.bodnor.serviceslicer.adapter.`in`.web.requests.InitiateFileUploadRequest
import cz.bodnor.serviceslicer.application.module.file.command.CompleteFileUploadCommand
import cz.bodnor.serviceslicer.application.module.file.command.ExtractZipFileCommand
import cz.bodnor.serviceslicer.application.module.file.command.FetchGitRepositoryCommand
import cz.bodnor.serviceslicer.application.module.file.command.InitiateFileUploadCommand
import cz.bodnor.serviceslicer.application.module.file.query.GetFileDownloadUrlQuery
import cz.bodnor.serviceslicer.application.module.file.query.ListFilesQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/files")
class FileController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {

    @GetMapping
    fun listFiles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ListFilesQuery.Result = queryBus(ListFilesQuery(page = page, size = size))

    @GetMapping("/{fileId}/download")
    fun getDownloadUrl(@PathVariable fileId: UUID): GetFileDownloadUrlQuery.Result =
        queryBus(GetFileDownloadUrlQuery(fileId = fileId))

    @PostMapping
    fun initiateUpload(@Valid @RequestBody request: InitiateFileUploadRequest): InitiateFileUploadCommand.Result =
        commandBus(request.toCommand())

    @PostMapping("/{fileId}/complete")
    fun completeUpload(@PathVariable fileId: UUID) {
        commandBus(CompleteFileUploadCommand(fileId))
    }

    @PostMapping("/{fileId}/extract")
    fun extractZipFile(@PathVariable fileId: UUID): ExtractZipFileCommand.Result =
        commandBus(ExtractZipFileCommand(fileId))

    @PostMapping("/git")
    fun fetchGitRepository(@Valid @RequestBody request: FetchGitRepositoryRequest): FetchGitRepositoryCommand.Result =
        commandBus(request.toCommand())
}
