package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.analysis.command.PrepareProjectRootCommand
import cz.bodnor.serviceslicer.application.module.file.FileService
import cz.bodnor.serviceslicer.application.module.project.service.ExtractZipFile
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.project.SourceType
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PrepareProjectRootCommandHandler(
    private val projectFinderService: ProjectFinderService,
    private val fileService: FileService,
    private val extractZipFile: ExtractZipFile,
    @Value("\${app.projects.working-dir}") private val projectWorkingDir: String,
) : CommandHandler<Unit, PrepareProjectRootCommand> {
    override val command = PrepareProjectRootCommand::class

    override fun handle(command: PrepareProjectRootCommand) {
        val project = projectFinderService.getById(command.projectId)

        when (project.sourceType) {
            SourceType.ZIP_FILE -> {
                val file = fileService.get(fileId = project.sourceFileId!!)

                val unzippedFolderPath = Path.of(projectWorkingDir, "${project.name}_${command.projectId}")

                extractZipFile(source = file, destination = unzippedFolderPath)

                project.setProjectRoot(unzippedFolderPath)
            }

            SourceType.GITHUB_REPOSITORY -> TODO()
        }
    }
}
