package cz.bodnor.serviceslicer.application.module.sut.command

import cz.bodnor.serviceslicer.domain.loadtestexperiment.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.loadtestexperiment.DockerConfig
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class AddSystemUnderTestCommand(
    val experimentId: UUID,
    val name: String,
    val description: String? = null,
    val isBaseline: Boolean,
    val dockerConfig: DockerConfig,
    val databaseSeedConfig: DatabaseSeedConfig? = null,
) : Command<AddSystemUnderTestCommand.Result> {

    data class Result(
        val systemUnderTestId: UUID,
    )
}

data class UpdateSystemUnderTestCommand(
    val experimentId: UUID,
    val sutId: UUID,
    val name: String,
    val description: String? = null,
    val dockerConfig: DockerConfig,
    val databaseSeedConfig: DatabaseSeedConfig? = null,
) : Command<UpdateSystemUnderTestCommand.Result> {

    data class Result(
        val systemUnderTestId: UUID,
    )
}

data class DeleteSystemUnderTestCommand(
    val experimentId: UUID,
    val sutId: UUID,
) : Command<Unit>
