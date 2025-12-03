package cz.bodnor.serviceslicer.application.module.benchmark.port.out

import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import java.util.UUID

interface GenerateBehaviorModels {

    operator fun invoke(openApiFileId: UUID): List<BehaviorModel>
}
