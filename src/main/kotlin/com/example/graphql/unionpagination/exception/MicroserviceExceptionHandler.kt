package com.example.graphql.unionpagination.exception

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class MicroserviceExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(MicroserviceException::class)
    protected fun handleMicroserviceException(
        ex: MicroserviceException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity(ex.error, ex.error.status)
    }

    /**
     * Override Reason: Prevent field errors from going back to clients, instead put in internal logging.
     */
    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        logger.warn("${MicroserviceErrors.INVALID_INPUT_DATA.code} [errors=${ex.message}]")
        return ResponseEntity(MicroserviceErrors.INVALID_INPUT_DATA, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    protected fun handleUnhandledException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled error occurred.", ex)
        return ResponseEntity(MicroserviceErrors.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
