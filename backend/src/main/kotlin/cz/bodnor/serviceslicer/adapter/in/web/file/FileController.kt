package cz.bodnor.serviceslicer.adapter.`in`.web.file

import cz.bodnor.serviceslicer.adapter.`in`.web.file.InitiateFileUploadRequest
import cz.bodnor.serviceslicer.application.module.file.command.CompleteFileUploadCommand
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
    private val mapper: FileMapper,
) {

    @GetMapping
    fun listFiles(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ListFilesResponse = mapper.toDto(queryBus(ListFilesQuery(page = page, size = size)))

    @GetMapping("/{fileId}/download")
    fun getDownloadUrl(@PathVariable fileId: UUID): GetDownloadUrlResponse =
        mapper.toDto(queryBus(GetFileDownloadUrlQuery(fileId)))

    @PostMapping
    fun initiateUpload(@Valid @RequestBody request: InitiateFileUploadRequest): InitiateFileUploadResponse =
        mapper.toDto(commandBus(mapper.toCommand(request)))

    @PostMapping("/{fileId}/complete")
    fun completeUpload(@PathVariable fileId: UUID): FileDto =
        mapper.toDto(commandBus(CompleteFileUploadCommand(fileId)))
}
