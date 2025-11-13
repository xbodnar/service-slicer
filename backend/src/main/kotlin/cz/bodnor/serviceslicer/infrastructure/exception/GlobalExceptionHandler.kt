package cz.bodnor.serviceslicer.infrastructure.exception

import org.bouncycastle.util.Objects
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(ApplicationError::class)
    fun handleAppErrors(exception: RuntimeException): ResponseEntity<Any> {
        return ResponseEntity.badRequest().body(exception.message)
    }
}
