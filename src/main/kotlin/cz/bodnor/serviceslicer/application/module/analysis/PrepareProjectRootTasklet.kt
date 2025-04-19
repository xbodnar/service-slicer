package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.application.module.file.FileService
import cz.bodnor.serviceslicer.application.module.project.service.ExtractZipFile
import cz.bodnor.serviceslicer.application.module.project.service.ProjectFinderService
import cz.bodnor.serviceslicer.domain.project.SourceType
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.UUID

@Component
@JobScope
class PrepareProjectRootTasklet(
    private val projectFinderService: ProjectFinderService,
    private val fileService: FileService,
    private val extractZipFile: ExtractZipFile,
    @Value("#{jobParameters['PROJECT_ID']}") private val projectId: UUID,
    @Value("\${app.projects.working-dir}") private val projectWorkingDir: String,
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus? {
        val project = projectFinderService.getById(projectId)

        when (project.sourceType) {
            SourceType.ZIP_FILE -> {
                val file = fileService.get(fileId = project.sourceFileId!!)

                val unzippedFolderPath = Path.of(projectWorkingDir, "${project.name}_$projectId")

                extractZipFile(source = file, destination = unzippedFolderPath)

                project.setProjectRoot(unzippedFolderPath)
            }

            SourceType.GITHUB_REPOSITORY -> TODO()
        }

        return RepeatStatus.FINISHED
    }
}
