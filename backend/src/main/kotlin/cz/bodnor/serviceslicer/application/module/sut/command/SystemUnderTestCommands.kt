package cz.bodnor.serviceslicer.application.module.sut.command

import cz.bodnor.serviceslicer.domain.sut.DatabaseSeedConfig
import cz.bodnor.serviceslicer.domain.sut.DockerConfig
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class CreateSystemUnderTestCommand(
    val name: String,
    val description: String?,
    val dockerConfig: DockerConfig,
    val databaseSeedConfigs: List<DatabaseSeedConfig>,
) : Command<SystemUnderTest>

data class UpdateSystemUnderTestCommand(
    val sutId: UUID,
    val name: String,
    val description: String? = null,
    val dockerConfig: DockerConfig,
    val databaseSeedConfigs: List<DatabaseSeedConfig> = emptyList(),
) : Command<SystemUnderTest>

data class DeleteSystemUnderTestCommand(
    val sutId: UUID,
) : Command<Unit>
