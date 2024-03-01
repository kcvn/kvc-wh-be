package com.kcvn.spm.common.helper

import java.nio.charset.StandardCharsets

class StringHelper {
    companion object {
        fun removeDecimalSuffix(value: String): String {
            return if (value.endsWith(".0")) {
                value.substringBeforeLast(".0")
            } else {
                value
            }
        }

        fun convertToUtf8(input: String?) : String? {
            if (input.isNullOrEmpty()) return null
            val utf8Bytes = input.toByteArray(StandardCharsets.UTF_8)
            return String(utf8Bytes, StandardCharsets.UTF_8)
        }
    }
}