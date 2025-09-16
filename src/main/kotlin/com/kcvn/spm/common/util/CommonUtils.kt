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
import java.time.LocalDateTime
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

//        fun parseDateSendings(dateStr: String?): LocalDate? {
//            return try {
//                if (!dateStr.isNullOrBlank()) {
//                    LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
//                } else {
//                    null
//                }
//            } catch (e: DateTimeParseException) {
//                throw e
//            }
//        }

        fun parseDateSending(dateStr: String?): LocalDate? {
            if (dateStr.isNullOrBlank()) return null

            val trimmed = dateStr.trim()

            return try {
                val pattern = when {
                    // yyyy/MM/dd
                    Regex("^\\d{4}/\\d{2}/\\d{2}$").matches(trimmed) ->
                        "yyyy/MM/dd"

                    // dd/MM/yyyy
                    Regex("^\\d{2}/\\d{2}/\\d{4}$").matches(trimmed) ->
                        "dd/MM/yyyy"

                    // dd/MM/yyyy HH:mm or HH:mm:ss
                    Regex("^\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}(:\\d{2})?$").matches(trimmed) ->
                        "dd/MM/yyyy HH:mm[:ss]"

                    // MM/dd/yyyy HH:mm or HH:mm:ss (nếu có dùng format kiểu Mỹ)
                    Regex("^\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}(:\\d{2})?$").matches(trimmed) ->
                        "MM/dd/yyyy HH:mm[:ss]"

                    else -> throw DateTimeParseException("Unrecognized date format", trimmed, 0)
                }

                if (pattern.contains("HH")) {
                    LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern(pattern)).toLocalDate()
                } else {
                    LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern))
                }
            } catch (e: DateTimeParseException) {
                throw e
            }
        }



        fun parseDateAmoeba(dateStr: String?): LocalDate? {
            return try {
                if (!dateStr.isNullOrBlank()) {
                    LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyy/MM/dd"))
                } else {
                    null
                }
            } catch (e: DateTimeParseException) {
                throw e
            }
        }

    }
}