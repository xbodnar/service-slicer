package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.command.DeleteSystemUnderTestCommand
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeleteSystemUnderTestCommandHandler(
    private val sutWriteService: SystemUnderTestWriteService,
) : CommandHandler<Unit, DeleteSystemUnderTestCommand> {

    override val command = DeleteSystemUnderTestCommand::class

    @Transactional
    override fun handle(command: DeleteSystemUnderTestCommand) {
        sutWriteService.delete(command.sutId)
    }
}
