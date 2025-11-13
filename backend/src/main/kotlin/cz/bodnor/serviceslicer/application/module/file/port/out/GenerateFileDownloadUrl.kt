package cz.bodnor.serviceslicer.application.module.file.port.out

interface GenerateFileDownloadUrl {

    operator fun invoke(storageKey: String): String
}
