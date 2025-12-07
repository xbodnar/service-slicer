package cz.bodnor.serviceslicer.adapter.`in`.web.sut

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileMapper
import cz.bodnor.serviceslicer.application.module.sut.command.CreateSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfigWithFile
import cz.bodnor.serviceslicer.domain.sut.DockerConfigWithFile
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWithFiles
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page
import java.util.UUID

@Mapper(componentModel = "spring", uses = [FileMapper::class])
interface SystemUnderTestMapper {

    // INBOUND MAPPINGS
    fun toCommand(request: CreateSystemUnderTestRequest): CreateSystemUnderTestCommand

    @Mapping(target = "sutId", source = "sutId")
    fun toCommand(
        request: UpdateSystemUnderTestRequest,
        sutId: UUID,
    ): UpdateSystemUnderTestCommand

    // OUTBOUND MAPPINGS
    fun toDto(result: SystemUnderTest): SystemUnderTestDto

    @Mapping(target = "items", source = "content", defaultExpression = "java(java.util.List.of())")
    @Mapping(target = "currentPage", source = "number")
    @Mapping(target = "pageSize", source = "size")
    fun toDto(result: Page<SystemUnderTest>): ListSystemsUnderTestResponse

    fun toDto(result: SystemUnderTestWithFiles): SystemUnderTestDetailDto

    fun toDto(dockerConfig: DockerConfigWithFile): DockerConfigDto

    fun toDto(databaseSeedConfig: DatabaseSeedConfigWithFile): DatabaseSeedConfigDto
}
