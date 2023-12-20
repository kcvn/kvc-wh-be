package com.kcvn.spm.common.util

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.text.Normalizer
import java.util.regex.Pattern

class CommonUtils {
    companion object {
        val REMOVE_ACCENT_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")

        fun loggedInUser(): String? {
            val authentication: Authentication = SecurityContextHolder.getContext().authentication
            return if (authentication !is AnonymousAuthenticationToken) {
                authentication.getName()
            } else null
        }

        fun removeAccent(s: String?): String {
            val temp: String = Normalizer.normalize(s, Normalizer.Form.NFD)
            return REMOVE_ACCENT_PATTERN.matcher(temp).replaceAll("")
                .replace('đ','d').replace('Đ','D')
        }
    }
}