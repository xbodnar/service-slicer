package cz.bodnor.serviceslicer.application.module.loadtest.port.out

import cz.bodnor.serviceslicer.domain.loadtest.BehaviorModel
import java.util.UUID

interface GenerateBehaviorModels {

    operator fun invoke(openApiFileId: UUID): List<BehaviorModel>
}
