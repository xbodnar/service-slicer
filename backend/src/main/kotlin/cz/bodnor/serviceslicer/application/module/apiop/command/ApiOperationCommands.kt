package cz.bodnor.serviceslicer.application.module.apiop.command

import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateApiOperationsCommand(
    val openApiFileId: UUID,
) : Command<List<ApiOperation>>
