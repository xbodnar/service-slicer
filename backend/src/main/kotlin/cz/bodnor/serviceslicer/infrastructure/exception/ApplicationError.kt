package cz.bodnor.serviceslicer.infrastructure.exception

class ApplicationError(message: String) : RuntimeException(message)

fun verify(
    condition: Boolean,
    message: (() -> String),
) {
    if (!condition) throw ApplicationError(message())
}

fun applicationError(message: String): Nothing = throw ApplicationError(message)
