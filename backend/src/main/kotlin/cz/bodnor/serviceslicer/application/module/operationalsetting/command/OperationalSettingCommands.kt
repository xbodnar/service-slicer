package cz.bodnor.serviceslicer.application.module.operationalsetting.command

import cz.bodnor.serviceslicer.domain.operationalsetting.BehaviorModel
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSetting
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.math.BigDecimal
import java.util.UUID

data class CreateOperationalSettingCommand(
    val name: String,
    val description: String? = null,
    val openApiFileId: UUID,
    val usageProfile: List<BehaviorModel>,
    val generateUsageProfile: Boolean = false,
    val operationalProfile: Map<Int, BigDecimal>,
) : Command<OperationalSetting>

data class DeleteOperationalSettingCommand(
    val operationalSettingId: UUID,
) : Command<Unit>

data class UpdateOperationalSettingCommand(
    val operationalSettingId: UUID,
    val name: String,
    val description: String? = null,
    val usageProfile: List<BehaviorModel>,
    val operationalProfile: Map<Int, BigDecimal>,
) : Command<OperationalSetting>
