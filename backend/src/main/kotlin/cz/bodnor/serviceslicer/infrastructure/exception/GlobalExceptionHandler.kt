package cz.bodnor.serviceslicer.infrastructure.exception

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(ApplicationError::class)
    fun handleAppErrors(exception: RuntimeException): ResponseEntity<Any> {
        logger.error(exception) { "Application error: ${exception.message}" }
        val problemDetail = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), exception.message)
        return ResponseEntity.badRequest().body(problemDetail)
    }
}
