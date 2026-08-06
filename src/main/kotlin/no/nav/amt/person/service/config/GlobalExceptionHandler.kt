package no.nav.amt.person.service.config

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler(
    @Value($$"${rest.include-stacktrace}") private val includeStacktrace: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(RuntimeException::class)
    fun handleException(
        ex: RuntimeException,
        request: HttpServletRequest,
    ): ResponseEntity<Response> = when (ex) {
        is NoSuchElementException -> buildResponse(HttpStatus.NOT_FOUND, ex)
        is IllegalStateException -> buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex)
        is AuthenticationException -> buildResponse(HttpStatus.UNAUTHORIZED, ex)
        is AccessDeniedException -> buildResponse(HttpStatus.FORBIDDEN, ex)
        is HttpMessageNotReadableException -> buildResponse(HttpStatus.BAD_REQUEST, ex)
        is ResponseStatusException -> buildResponse(HttpStatus.valueOf(ex.statusCode.value()), ex)

        else -> {
            log.error("Internal server error - ${ex.message} - ${request.method}: ${request.requestURI}", ex)
            buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex)
        }
    }

    private fun buildResponse(
        status: HttpStatus,
        exception: Throwable,
    ): ResponseEntity<Response> = ResponseEntity
        .status(status)
        .body(
            Response(
                status = status.value(),
                title = status,
                detail = exception.message,
                stacktrace = if (includeStacktrace) exception.stackTraceToString() else null,
            ),
        )

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Response(
        val status: Int,
        val title: HttpStatus,
        val detail: String?,
        val stacktrace: String? = null,
    )
}
