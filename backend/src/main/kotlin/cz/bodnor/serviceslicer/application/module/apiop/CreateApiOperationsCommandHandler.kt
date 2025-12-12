package cz.bodnor.serviceslicer.application.module.apiop

import cz.bodnor.serviceslicer.application.module.apiop.command.CreateApiOperationsCommand
import cz.bodnor.serviceslicer.application.module.operationalsetting.service.OpenApiParsingService
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.domain.apiop.ApiOperationWriteService
import cz.bodnor.serviceslicer.domain.file.FileReadService
import cz.bodnor.serviceslicer.domain.file.FileStatus
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import cz.bodnor.serviceslicer.infrastructure.exception.verify
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CreateApiOperationsCommandHandler(
    private val openApiParsingService: OpenApiParsingService,
    private val fileReadService: FileReadService,
    private val apiOperationWriteService: ApiOperationWriteService,
) : CommandHandler<List<ApiOperation>, CreateApiOperationsCommand> {

    override val command = CreateApiOperationsCommand::class

    @Transactional
    override fun handle(command: CreateApiOperationsCommand): List<ApiOperation> {
        val file = fileReadService.getById(command.openApiFileId)
        verify(file.status == FileStatus.READY) { "File is not uploaded yet" }

        val apiOperations = openApiParsingService.parse(file.id)

        require(apiOperations.isNotEmpty()) { "Failed to parse API operations from file ${file.id}" }

        return apiOperationWriteService.save(apiOperations)
    }
}
