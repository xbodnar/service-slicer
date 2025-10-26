package cz.bodnor.serviceslicer.application.module.project.command

import cz.bodnor.serviceslicer.domain.projectsource.SourceType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.Command
import java.nio.file.Path
import java.util.UUID

data class CreateProjectCommand private constructor(
    val projectName: String,
    val basePackageName: String,
    val file: Path? = null,
    val projectRootRelativePath: Path = Path.of(""),
    val gitUri: String? = null,
    val branchName: String? = null,
    val sourceType: SourceType,
) : Command<CreateProjectCommand.CreateProjectResult> {

    data class CreateProjectResult(
        val projectId: UUID,
    )

    companion object {
        fun fromZip(
            projectName: String,
            basePackageName: String,
            file: Path,
            projectRootRelativePath: Path = Path.of(""),
        ) = CreateProjectCommand(
            projectName = projectName,
            basePackageName = basePackageName,
            file = file,
            projectRootRelativePath = projectRootRelativePath,
            sourceType = SourceType.ZIP,
        )

        fun fromGit(
            projectName: String,
            basePackageName: String,
            gitUri: String,
            projectRootRelativePath: Path = Path.of(""),
            branchName: String = "main",
        ) = CreateProjectCommand(
            projectName = projectName,
            basePackageName = basePackageName,
            gitUri = gitUri,
            projectRootRelativePath = projectRootRelativePath,
            branchName = branchName,
            sourceType = SourceType.GIT,
        )

        fun fromJar(
            projectName: String,
            basePackageName: String,
            file: Path,
        ) = CreateProjectCommand(
            projectName = projectName,
            basePackageName = basePackageName,
            file = file,
            sourceType = SourceType.JAR,
        )
    }
}

data class InitializeProjectCommand(
    val projectId: UUID,
) : Command<Unit>
