package cz.bodnor.serviceslicer.infrastructure.exception

class ApplicationError(message: String) : RuntimeException() {
}

fun verify(condition: Boolean, message: (() -> String)) {
    if (!condition) throw ApplicationError(message())
}
