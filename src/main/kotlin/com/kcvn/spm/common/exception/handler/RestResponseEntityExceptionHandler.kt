package com.kcvn.spm.common.exception.handler

import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {
    private val loggerKotlin = KotlinLogging.logger {}

    @ExceptionHandler(value = [BadCredentialsException::class])
    protected fun handleBadCredentialsException(ex: RuntimeException, request: ServletWebRequest): ResponseEntity<Any>? {
        val bodyOfResponse: MutableMap<String, Any> = HashMap()
        bodyOfResponse["status"] = HttpServletResponse.SC_BAD_REQUEST
        bodyOfResponse["error"] = HttpStatus.BAD_REQUEST.reasonPhrase
        bodyOfResponse["message"] = CommonUtils.getMessage("login.error")
        bodyOfResponse["path"] = request.request.servletPath
        return handleExceptionInternal(
            ex,
            bodyOfResponse,
            HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        )
    }

    @ExceptionHandler(value = [BusinessException::class])
    protected fun handleBusinessException(ex: RuntimeException, request: ServletWebRequest): ResponseEntity<Any>? {
        val bodyOfResponse: MutableMap<String, Any> = HashMap()
        bodyOfResponse["status"] = HttpServletResponse.SC_BAD_REQUEST
        bodyOfResponse["error"] = HttpStatus.BAD_REQUEST.reasonPhrase
        bodyOfResponse["message"] = ex.localizedMessage
        bodyOfResponse["path"] = request.request.servletPath
        return handleExceptionInternal(
            ex,
            bodyOfResponse,
            HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        )
    }

    @ExceptionHandler(BusinessExceptionDetail::class)
    fun handleBusinessException(ex: BusinessExceptionDetail): ResponseEntity<MessageResponse> {
        loggerKotlin.error { "BUSINESS EXCEPTION: ${ex.message}" + " --- DATA: ${ex.data}" }
        return ResponseEntity(MessageResponse(ex.message ?: "UNKNOWN ERROR"), HttpStatus.BAD_REQUEST)
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val firstErrorMessage = ex.bindingResult.fieldErrors.firstOrNull()?.defaultMessage.toString()

        val result: Map<String, String?> = mapOf("message" to CommonUtils.getMessage(firstErrorMessage))

        return ResponseEntity(result, HttpStatus.BAD_REQUEST)
    }
}