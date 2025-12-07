package cz.bodnor.serviceslicer.adapter.`in`.web.file

import cz.bodnor.serviceslicer.application.module.file.command.InitiateFileUploadCommand
import cz.bodnor.serviceslicer.application.module.file.query.GetFileDownloadUrlQuery
import cz.bodnor.serviceslicer.domain.file.File
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page

@Mapper(componentModel = "spring")
interface FileMapper {

    // INBOUND MAPPINGS

    fun toCommand(request: InitiateFileUploadRequest): InitiateFileUploadCommand

    // OUTBOUND MAPPINGS

    fun toDto(result: File): FileDto

    @Mapping(target = "items", source = "content", defaultExpression = "java(java.util.List.of())")
    @Mapping(target = "currentPage", source = "number")
    @Mapping(target = "pageSize", source = "size")
    fun toDto(result: Page<File>): ListFilesResponse

    fun toDto(result: InitiateFileUploadCommand.Result): InitiateFileUploadResponse

    fun toDto(result: GetFileDownloadUrlQuery.Result): GetDownloadUrlResponse
}
