package cz.bodnor.serviceslicer.application.module.file.port.out

interface GenerateFileUploadUrl {

    operator fun invoke(storageKey: String): String
}
