package com.kcvn.spm.app.auth.controller.handler

import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {
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
}