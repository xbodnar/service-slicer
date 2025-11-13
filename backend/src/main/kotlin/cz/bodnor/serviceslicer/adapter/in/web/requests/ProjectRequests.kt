package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectCommand
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Request to create a new project for analysis")
data class CreateProjectRequest(
    @Schema(description = "Name of the project", example = "MyJavaApp")
    val projectName: String,
    @Schema(description = "Base package name to analyze", example = "com.example.myapp")
    val basePackageName: String,
    @Schema(description = "List of package names to exclude from analysis", example = "[\"com.example.myapp.test\"]")
    val excludePackages: List<String>,
    @Schema(description = "ID of the uploaded JAR file")
    val jarFileId: UUID,
    @Schema(description = "ID of the uploaded project directory (optional)")
    val projectDirId: UUID?,
) {
    fun toCommand(): CreateProjectCommand = CreateProjectCommand(
        projectName = projectName,
        basePackageName = basePackageName,
        excludePackages = excludePackages,
        jarFileId = jarFileId,
        projectDirId = projectDirId,
    )
}
