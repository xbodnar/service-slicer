package cz.bodnor.serviceslicer.application.module.sut.command

import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.util.UUID

data class UpdateSystemUnderTestCommand(
    val experimentId: UUID,
    val sutId: UUID,
    val name: String,
    val composeFileId: UUID,
    val jarFileId: UUID,
    val sqlSeedFileId: UUID? = null,
    val description: String? = null,
    val healthCheckPath: String = "/actuator/health",
    val appPort: Int = 9090,
    val startupTimeoutSeconds: Long = 180,
    val dbContainerName: String? = null,
    val dbPort: Int? = null,
    val dbName: String? = null,
    val dbUsername: String? = null,
) : Command<UpdateSystemUnderTestCommand.Result> {

    data class Result(
        val systemUnderTestId: UUID,
    )
}

data class DeleteSystemUnderTestCommand(
    val experimentId: UUID,
    val sutId: UUID,
) : Command<Unit>

data class AddSystemUnderTestCommand(
    val experimentId: UUID,
    val name: String,
    val composeFileId: UUID,
    val jarFileId: UUID,
    val sqlSeedFileId: UUID? = null,
    val description: String? = null,
    val healthCheckPath: String = "/actuator/health",
    val appPort: Int = 9090,
    val startupTimeoutSeconds: Long = 180,
    val dbContainerName: String? = null,
    val dbPort: Int? = null,
    val dbName: String? = null,
    val dbUsername: String? = null,
) : Command<AddSystemUnderTestCommand.Result> {

    data class Result(
        val systemUnderTestId: UUID,
    )
}
