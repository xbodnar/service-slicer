package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.application.module.file.FileFinderService
import cz.bodnor.serviceslicer.application.module.file.FileService
import cz.bodnor.serviceslicer.application.module.project.service.ExtractZipFile
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PrepareProjectRootFromZip(
    private val fileService: FileService,
    private val fileFinderService: FileFinderService,
    private val extractZipFile: ExtractZipFile,
    @Value("\${app.projects.working-dir}") private val projectWorkingDir: String,
) {

    operator fun invoke(source: ZipFileProjectSource): Path {
        val file = fileService.get(fileId = source.fileId)
        val originalFileName = fileFinderService.getById(source.fileId).originalFileName

        val unzippedFolderPath = Path.of(projectWorkingDir, source.projectId.toString())

        extractZipFile(source = file, destination = unzippedFolderPath)

        return unzippedFolderPath.resolve(originalFileName)
    }
}
