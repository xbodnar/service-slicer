package cz.bodnor.serviceslicer.infrastructure.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    @ExceptionHandler(ApplicationError::class)
    fun handleAppErrors(exception: RuntimeException): ResponseEntity<Any> =
        ResponseEntity.badRequest().body(exception.message)
}
