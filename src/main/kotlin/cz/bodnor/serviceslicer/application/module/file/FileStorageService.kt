package cz.bodnor.serviceslicer.application.module.file

import java.io.InputStream
import java.nio.file.Path

interface FileStorageService {

    fun save(
        fileName: String,
        file: InputStream,
    )

    fun get(fileName: String): Path
}
