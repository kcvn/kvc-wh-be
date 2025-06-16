package com.kcvn.spm.common.util

import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.lang.Nullable
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.regex.Pattern

class CommonUtils {
    companion object {
        private val REMOVE_ACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

        fun loggedInUser(): String? {
            val authentication: Authentication = SecurityContextHolder.getContext().authentication
            return if (authentication !is AnonymousAuthenticationToken) {
                authentication.getName()
            } else null
        }

        fun removeAccent(s: String?): String? {
            if (s == null) return null
            val temp: String = Normalizer.normalize(s, Normalizer.Form.NFD)
            return REMOVE_ACCENT_PATTERN.matcher(temp).replaceAll("")
                .replace('đ', 'd').replace('Đ', 'D')
        }

        private fun getMessageResource(): MessageSource {
            val messageSource = ResourceBundleMessageSource()
            messageSource.setBasenames("messages")
            messageSource.setDefaultEncoding("UTF-8")
            messageSource.setUseCodeAsDefaultMessage(true)
            return messageSource
        }

        fun getMessage(code: String, @Nullable args: Array<Any>): String =
            getMessageResource().getMessage(code, args, LocaleContextHolder.getLocale())

        fun getMessage(code: String): String =
            getMessageResource().getMessage(code, null, LocaleContextHolder.getLocale())

        fun parseDate(dateStr: String?): LocalDate? {
            return try {
                if (!dateStr.isNullOrBlank()) {
                    LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } else {
                    null
                }
            } catch (e: DateTimeParseException) {
                throw e
            }
        }

    }
}