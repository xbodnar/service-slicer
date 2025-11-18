package cz.bodnor.serviceslicer.application.module.loadtest.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class ExecuteLoadTestCommand(
    val experimentId: UUID,
    val systemUnderTestId: UUID,
    val targetVus: Int,
) : Command<ExecuteLoadTestCommand.Result> {

    data class Result(
        val summaryJson: String,
        val exitCode: Int,
        val stdOut: String,
    )
}
