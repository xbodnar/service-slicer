package cz.bodnor.serviceslicer.adapter.`in`.web.sut

import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.query.GetSystemUnderTestQuery
import cz.bodnor.serviceslicer.application.module.sut.query.ListSystemsUnderTestQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/systems-under-test")
class SystemUnderTestController(
    private val queryBus: QueryBus,
    private val commandBus: CommandBus,
    private val mapper: SystemUnderTestMapper,
) {

    @GetMapping
    fun listSystemsUnderTest(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ListSystemsUnderTestResponse = mapper.toDto(queryBus(ListSystemsUnderTestQuery(page = page, size = size)))

    @GetMapping("/{sutId}")
    fun getSystemUnderTest(@PathVariable sutId: UUID): SystemUnderTestDetailDto =
        mapper.toDto(queryBus(GetSystemUnderTestQuery(sutId = sutId)))

    @PostMapping
    fun createSystemUnderTest(@RequestBody request: CreateSystemUnderTestRequest): SystemUnderTestDto =
        mapper.toDto(commandBus(mapper.toCommand(request)))

    @DeleteMapping("/{sutId}")
    fun deleteSystemUnderTest(@PathVariable sutId: UUID) = commandBus(DeleteSystemUnderTestCommand(sutId = sutId))

    @PutMapping("/{sutId}")
    fun updateSystemUnderTest(
        @PathVariable sutId: UUID,
        @RequestBody request: UpdateSystemUnderTestRequest,
    ): SystemUnderTestDto = mapper.toDto(commandBus(mapper.toCommand(request, sutId)))
}
