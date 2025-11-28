package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.AddSystemUnderTestRequest
import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/benchmarks/{benchmarkId}/sut")
class SystemUnderTestController(
    private val commandBus: CommandBus,
) {

    @PostMapping
    fun addSystemUnderTest(
        @PathVariable benchmarkId: UUID,
        @RequestBody request: AddSystemUnderTestRequest,
    ): AddSystemUnderTestCommand.Result = commandBus(request.toCommand(benchmarkId))

    @DeleteMapping("/{sutId}")
    fun deleteSystemUnderTest(
        @PathVariable benchmarkId: UUID,
        @PathVariable sutId: UUID,
    ) = commandBus(DeleteSystemUnderTestCommand(benchmarkId = benchmarkId, sutId = sutId))
}
