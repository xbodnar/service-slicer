package cz.bodnor.serviceslicer.application.module.project.service

import cz.bodnor.serviceslicer.application.module.file.FileService
import org.springframework.stereotype.Component
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import kotlin.io.path.extension

@Component
class ExtractZipFile(
    private val fileService: FileService,
) {

    operator fun invoke(
        source: Path,
        destination: Path,
    ) {
        require(source.extension == "zip") { "Invalid file format - must end with .zip" }

        // Create destination directory if it doesn't exist
        if (!Files.exists(destination)) {
            Files.createDirectories(destination)
        }

        // Open the zip file
        ZipInputStream(FileInputStream(source.toFile())).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry

            // Iterate through each entry in the zip file
            while (zipEntry != null) {
                val entryPath = destination.resolve(zipEntry.name)

                // Create directories if needed
                if (zipEntry.isDirectory) {
                    Files.createDirectories(entryPath)
                } else {
                    // Create parent directories if needed
                    Files.createDirectories(entryPath.parent)

                    // Extract the file
                    Files.newOutputStream(entryPath).use { outputStream ->
                        zipInputStream.copyTo(outputStream)
                    }
                }

                // Close the current entry and move to the next one
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
        }
    }
}
