package cz.bodnor.serviceslicer.adapter.`in`.web.requests

import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectCommand
import java.nio.file.Path

data class CreateProjectFromZipRequest(
    val projectName: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val projectRootRelativePath: Path = Path.of(""),
    val file: Path,
) {
    fun toCommand() = CreateProjectCommand.fromZip(
        projectName = projectName,
        basePackageName = basePackageName,
        excludePackages = excludePackages,
        file = file,
        projectRootRelativePath = projectRootRelativePath,
    )
}

data class CreateProjectFromGitRequest(
    val projectName: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val projectRootRelativePath: Path = Path.of(""),
    val gitUri: String,
    val branchName: String,
) {
    fun toCommand() = CreateProjectCommand.fromGit(
        projectName = projectName,
        basePackageName = basePackageName,
        excludePackages = excludePackages,
        gitUri = gitUri,
        projectRootRelativePath = projectRootRelativePath,
        branchName = branchName,
    )
}

data class CreateProjectFromJarRequest(
    val projectName: String,
    val basePackageName: String,
    val excludePackages: List<String>,
    val file: Path,
) {
    fun toCommand() = CreateProjectCommand.fromJar(
        projectName = projectName,
        basePackageName = basePackageName,
        excludePackages = excludePackages,
        file = file,
    )
}
