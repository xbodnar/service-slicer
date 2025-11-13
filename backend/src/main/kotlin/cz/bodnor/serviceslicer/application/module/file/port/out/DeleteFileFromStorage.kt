package cz.bodnor.serviceslicer.application.module.file.port.out

interface DeleteFileFromStorage {

    operator fun invoke(storageKey: String)
}
