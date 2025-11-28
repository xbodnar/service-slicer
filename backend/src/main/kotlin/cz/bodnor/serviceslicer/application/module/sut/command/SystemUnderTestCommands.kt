package cz.bodnor.serviceslicer.application.module.sut.command

import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class AddSystemUnderTestCommand(
    val benchmarkId: UUID,
    val name: String,
    val description: String? = null,
    val isBaseline: Boolean,
    val dockerConfig: DockerConfig,
    val databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) : Command<AddSystemUnderTestCommand.Result> {

    data class Result(
        val systemUnderTestId: UUID,
    )
}

data class UpdateSystemUnderTestCommand(
    val benchmarkId: UUID,
    val sutId: UUID,
    val name: String,
    val description: String? = null,
    val dockerConfig: DockerConfig,
    val databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) : Command<UpdateSystemUnderTestCommand.Result> {

    data class Result(
        val systemUnderTestId: UUID,
    )
}

data class DeleteSystemUnderTestCommand(
    val benchmarkId: UUID,
    val sutId: UUID,
) : Command<Unit>
