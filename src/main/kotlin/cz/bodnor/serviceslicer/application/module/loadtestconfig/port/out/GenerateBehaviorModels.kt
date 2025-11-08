package cz.bodnor.serviceslicer.application.module.loadtestconfig.port.out

import cz.bodnor.serviceslicer.domain.loadtestconfig.BehaviorModel
import java.util.UUID

interface GenerateBehaviorModels {

    operator fun invoke(openApiFileId: UUID): List<BehaviorModel>
}
