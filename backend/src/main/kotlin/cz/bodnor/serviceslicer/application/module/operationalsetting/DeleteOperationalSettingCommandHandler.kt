package cz.bodnor.serviceslicer.application.module.operationalsetting

import cz.bodnor.serviceslicer.application.module.operationalsetting.command.DeleteOperationalSettingCommand
import cz.bodnor.serviceslicer.domain.operationalsetting.OperationalSettingWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteOperationalSettingCommandHandler(
    private val operationalSettingWriteService: OperationalSettingWriteService,
) : CommandHandler<Unit, DeleteOperationalSettingCommand> {
    override val command = DeleteOperationalSettingCommand::class

    @Transactional
    override fun handle(command: DeleteOperationalSettingCommand) {
        operationalSettingWriteService.delete(command.operationalSettingId)
    }
}
