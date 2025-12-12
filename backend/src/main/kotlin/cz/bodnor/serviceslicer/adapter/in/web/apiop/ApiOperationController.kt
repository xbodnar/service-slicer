package cz.bodnor.serviceslicer.adapter.`in`.web.apiop

import cz.bodnor.serviceslicer.application.module.apiop.command.CreateApiOperationsCommand
import cz.bodnor.serviceslicer.application.module.apiop.query.ListApiOperationsQuery
import cz.bodnor.serviceslicer.domain.apiop.ApiOperation
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api-operations")
class ApiOperationController(
    private val queryBus: QueryBus,
    private val commandBus: CommandBus,
) {

    @GetMapping
    fun listApiOperations(@RequestParam openApiFileId: UUID) = queryBus(ListApiOperationsQuery(openApiFileId))

    @PostMapping
    fun createApiOperations(@RequestParam openApiFileId: UUID): List<ApiOperation> =
        commandBus(CreateApiOperationsCommand(openApiFileId))
}
