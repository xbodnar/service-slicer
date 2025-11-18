package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.AddSystemUnderTestRequest
import cz.bodnor.serviceslicer.adapter.`in`.web.requests.UpdateSystemUnderTestRequest
import cz.bodnor.serviceslicer.application.module.loadtest.command.ExecuteLoadTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/load-tests/experiments/{experimentId}/sut")
class SystemUnderTestController(
    private val commandBus: CommandBus,
) {

    @PostMapping
    fun addSystemUnderTest(
        @PathVariable experimentId: UUID,
        @RequestBody request: AddSystemUnderTestRequest,
    ) = commandBus(request.toCommand(experimentId))

    @PutMapping("/{sutId}")
    fun updateSystemUnderTest(
        @PathVariable experimentId: UUID,
        @PathVariable sutId: UUID,
        @RequestBody request: UpdateSystemUnderTestRequest,
    ) = commandBus(request.toCommand(experimentId, sutId))

    @DeleteMapping("/{sutId}")
    fun deleteSystemUnderTest(
        @PathVariable experimentId: UUID,
        @PathVariable sutId: UUID,
    ) = commandBus(DeleteSystemUnderTestCommand(experimentId, sutId))

    @PostMapping("/{sutId}/execute")
    fun startSystemUnderTest(
        @PathVariable experimentId: UUID,
        @PathVariable sutId: UUID,
    ) = commandBus(ExecuteLoadTestCommand(experimentId, sutId, 10))
}
