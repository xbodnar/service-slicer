package cz.bodnor.serviceslicer.application.module.loadtest.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class ExecuteArchitectureLoadTestCommand(
    val experimentId: UUID,
    val systemUnderTestId: UUID,
    val targetVus: Int,
) : Command<Unit>
