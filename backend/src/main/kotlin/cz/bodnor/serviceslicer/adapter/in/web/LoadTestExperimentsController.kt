package cz.bodnor.serviceslicer.adapter.`in`.web

import cz.bodnor.serviceslicer.adapter.`in`.web.requests.CreateLoadTestExperimentRequest
import cz.bodnor.serviceslicer.adapter.`in`.web.requests.UpdateLoadTestConfigRequest
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.command.GenerateBehaviorModelsCommand
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.GetLoadTestExperimentQuery
import cz.bodnor.serviceslicer.application.module.loadtestexperiment.query.ListLoadTestExperimentsQuery
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryBus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/load-tests/experiments")
class LoadTestExperimentsController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {

    @GetMapping
    fun listExperiments(): ListLoadTestExperimentsQuery.Result = queryBus(ListLoadTestExperimentsQuery())

    @PostMapping
    fun createExperiment(@RequestBody request: CreateLoadTestExperimentRequest) = commandBus(request.toCommand())

    @GetMapping("/{experimentId}")
    fun getExperiment(@PathVariable experimentId: UUID): GetLoadTestExperimentQuery.Result =
        queryBus(GetLoadTestExperimentQuery(experimentId = experimentId))

    @PutMapping("/{experimentId}/config")
    fun updateLoadTestConfig(
        @PathVariable experimentId: UUID,
        @RequestBody request: UpdateLoadTestConfigRequest,
    ) = commandBus(request.toCommand(experimentId))

    @PostMapping("/{experimentId}/config/generate-bm")
    fun generateBehaviorModels(
        @PathVariable experimentId: UUID,
    ) = commandBus(GenerateBehaviorModelsCommand(experimentId = experimentId))
}
