package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.sut.command.AddSystemUnderTestCommand
import cz.bodnor.serviceslicer.application.module.sut.command.UpdateSystemUnderTestCommand
import java.util.UUID

data class AddSystemUnderTestRequest(
    val name: String,
    val composeFileId: UUID,
    val jarFileId: UUID,
    val description: String? = null,
    val healthCheckPath: String = "/actuator/health",
    val appPort: Int = 9090,
    val startupTimeoutSeconds: Long = 180,
) {
    fun toCommand(experimentId: UUID) = AddSystemUnderTestCommand(
        experimentId = experimentId,
        name = name,
        composeFileId = composeFileId,
        jarFileId = jarFileId,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}

data class UpdateSystemUnderTestRequest(
    val name: String,
    val composeFileId: UUID,
    val jarFileId: UUID,
    val description: String? = null,
    val healthCheckPath: String = "/actuator/health",
    val appPort: Int = 9090,
    val startupTimeoutSeconds: Long = 180,
) {
    fun toCommand(
        experimentId: UUID,
        sutId: UUID,
    ) = UpdateSystemUnderTestCommand(
        experimentId = experimentId,
        sutId = sutId,
        name = name,
        composeFileId = composeFileId,
        jarFileId = jarFileId,
        description = description,
        healthCheckPath = healthCheckPath,
        appPort = appPort,
        startupTimeoutSeconds = startupTimeoutSeconds,
    )
}
