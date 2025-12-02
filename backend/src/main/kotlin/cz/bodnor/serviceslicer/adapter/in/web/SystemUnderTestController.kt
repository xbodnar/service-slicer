package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateSystemUnderTestRequest
import cz.bodnor.serviceslicer.adapter.`in`.web.requests.UpdateSystemUnderTestRequest
import cz.bodnor.serviceslicer.application.module.sut.command.CreateSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.query.GetSystemUnderTestQuery
import cz.bodnor.serviceslicer.application.module.sut.query.ListSystemsUnderTestQuery
import cz.bodnor.serviceslicer.application.module.sut.query.SystemUnderTestDto
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/systems-under-test")
class SystemUnderTestController(
    private val queryBus: QueryBus,
    private val commandBus: CommandBus,
) {

    @GetMapping
    fun listSystemsUnderTest(): ListSystemsUnderTestQuery.Result = queryBus(ListSystemsUnderTestQuery())

    @PostMapping
    fun createSystemUnderTest(
        @RequestBody request: CreateSystemUnderTestRequest,
    ): CreateSystemUnderTestCommand.Result = commandBus(request.toCommand())

    @GetMapping("/{sutId}")
    fun getSystemUnderTest(@PathVariable sutId: UUID): SystemUnderTestDto =
        queryBus(GetSystemUnderTestQuery(sutId = sutId))

    @DeleteMapping("/{sutId}")
    fun deleteSystemUnderTest(@PathVariable sutId: UUID) = commandBus(DeleteSystemUnderTestCommand(sutId = sutId))

    @PutMapping("/{sutId}")
    fun updateSystemUnderTest(
        @PathVariable sutId: UUID,
        @RequestBody request: UpdateSystemUnderTestRequest,
    ): UpdateSystemUnderTestCommand.Result = commandBus(request.toCommand(sutId))
}
